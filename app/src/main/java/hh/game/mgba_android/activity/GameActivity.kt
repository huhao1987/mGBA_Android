package hh.game.mgba_android.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import hh.game.mgba_android.R
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.fragment.MemorySearchFragment
import hh.game.mgba_android.fragment.OnAddressClickListener
import hh.game.mgba_android.fragment.OnDialogClickListener
import hh.game.mgba_android.fragment.OnMemSearchListener
import hh.game.mgba_android.fragment.PopDialogFragment
import hh.game.mgba_android.memory.CoreMemoryBlock
import hh.game.mgba_android.utils.CheatUtils
import hh.game.mgba_android.utils.GBAKeys
import hh.game.mgba_android.utils.Gametype
import hh.game.mgba_android.utils.controllerUtil.getDirectionPressed
import hh.game.mgba_android.utils.controllerUtil.lastDirect
import hh.game.mgba_android.utils.getKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libsdl.app.SDLUtils
import org.libsdl.app.SDLUtils.mFullscreenModeActive
import org.libsdl.app.SDLUtils.onNativeKeyDown
import org.libsdl.app.SDLUtils.onNativeKeyUp
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class GameActivity : AppCompatActivity() {
    private var runFPS = true
    private var setFPS = 60f
    private var isMute = false
    private var templateResult = ArrayList<Pair<Int, Int>>()
    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val gameNum = intent.getStringExtra("cheat")
                var internalCheatFile =
                    getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
                reCallCheats(internalCheatFile)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFullscreenModeActive = false
        setContentView(R.layout.activity_game)
        var gamepath = intent.getStringExtra("gamepath")
        val gameNum = intent.getStringExtra("cheat")
        Log.d("GameActivity", "onCreate: gameNum='$gameNum', gamepath='$gamepath'")
        var cheatpath = gamepath?.replace(".gba", ".cheats")
        if (!File(cheatpath).exists()) cheatpath = null
        var internalCheatFile = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"

        var fragmentShader = "uniform sampler2D tex;\n" +
                "uniform vec2 texSize;\n" +
                "varying vec2 texCoord;\n" +
                "\n" +
                "uniform float boundBrightness;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "\tvec4 color = texture2D(tex, texCoord);\n" +
                "\n" +
                "\tif (int(mod(texCoord.s * texSize.x * 3.0, 3.0)) == 0 ||\n" +
                "\t\tint(mod(texCoord.t * texSize.y * 3.0, 3.0)) == 0)\n" +
                "\t{\n" +
                "\t\tcolor.rgb *= vec3(1.0, 1.0, 1.0) * boundBrightness;\n" +
                "\t}\n" +
                "\n" +
                "\tgl_FragColor = color;\n" +
                "}"
        if (gamepath != null)
            CheatUtils.generateCheat(this, gameNum, cheatpath)
        SDLUtils.init(this, findViewById(R.id.gameView))
            .setLibraries(
                "SDL2",
                "mgba",
                "mgba_android"
            )
            .setArguments(
                gamepath,
                internalCheatFile,
//                fragmentShader
            )
        addGameControler()
//        GlobalScope.launch {
//            Gameutils.getFPS().toString()
//        }
        initSwappy()
        
        // Copy shaders from assets to files dir
        // Always copy to ensure we have the latest shaders (e.g. after app update)
        val shaderDir = File(filesDir, "shaders")
        copyAssets("shaders", shaderDir.absolutePath)
        
        // Load xBRZ shader
        val shaderBtn = findViewById<TextView>(R.id.shader_btn)
        var currentShaderName = "None"
        
        shaderBtn.setOnClickListener {
            val shaderDir = File(filesDir, "shaders")
            if (!shaderDir.exists()) shaderDir.mkdirs()
            
            // Filter for directories (shaders are usually folders with manifest.ini) or .shader files
            val shaderFiles = shaderDir.listFiles { file -> 
                file.isDirectory || file.extension == "shader" 
            }?.map { it.name }?.toMutableList() ?: mutableListOf()
            
            shaderFiles.add(0, "Clear")
            
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Shader")
            builder.setItems(shaderFiles.toTypedArray()) { dialog, which ->
                val selectedName = shaderFiles[which]
                if (selectedName == "Clear") {
                    setShader("")
                    currentShaderName = "None"
                    shaderBtn.text = "Set Shader"
                    shaderBtn.setBackgroundColor(0x800000FF.toInt()) // Blue
                } else {
                    val selectedFile = File(shaderDir, selectedName)
                    val path = selectedFile.absolutePath
                    val success = setShader(path)
                    if (success) {
                        currentShaderName = selectedName
                        shaderBtn.text = "Shader: $selectedName"
                        shaderBtn.setBackgroundColor(0x8000FF00.toInt()) // Green
                    } else {
                        shaderBtn.text = "Shader: ERR"
                        shaderBtn.setBackgroundColor(0x80FF0000.toInt()) // Red
                    }
                }
            }
            builder.show()
        }

        val fpsText = findViewById<TextView>(R.id.fps_text)
        lifecycleScope.launch(Dispatchers.Main) {
            while (runFPS) {
                fpsText.text = "FPS: %.1f".format(getFPS())
                delay(500)
            }
        }
    }

    private fun copyAssets(assetPath: String, destPath: String) {
        val assetManager = assets
        var files: Array<String>? = null
        try {
            files = assetManager.list(assetPath)
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
        if (files != null) {
            if (files.isEmpty()) {
                // It's a file
                try {
                    val `in` = assetManager.open(assetPath)
                    val out = java.io.FileOutputStream(destPath)
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (`in`.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    `in`.close()
                    out.flush()
                    out.close()
                } catch (e: java.io.IOException) {
                    e.printStackTrace()
                }
            } else {
                // It's a directory
                val dir = File(destPath)
                if (!dir.exists()) dir.mkdirs()
                for (fileName in files) {
                    copyAssets(
                        if (assetPath == "") fileName else "$assetPath/$fileName",
                        "$destPath/$fileName"
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runFPS = false
//        GlobalScope.cancel()
    }

    override fun onPause() {
        super.onPause()
        PauseGame()
    }

    override fun onResume() {
        super.onResume()
        ResumeGame()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
//        Game controler
        var handled = false
        var gbaKey = getKey(event.keyCode)
        if (gbaKey != GBAKeys.GBA_KEY_NONE.key) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    onNativeKeyDown(gbaKey)
                    handled = true
                }

                KeyEvent.ACTION_UP -> {
                    onNativeKeyUp(gbaKey)
                    handled = true
                }

            }
        }
        return handled || SDLUtils.dispatchKeyEvent(event)
    }

    private fun addGameControler() {
        val gameNum = intent.getStringExtra("cheat")
        findViewById<TextView>(R.id.cheatbtn).setOnClickListener {
            startForResult.launch(Intent(this, CheatsActivity::class.java).also {
                when (intent.getStringExtra("gametype")) {
                    "GBA" ->
                        intent.getParcelableExtra<GBAgame>("gamedetail").let { game ->
                            it.putExtra(
                                "gamedetail",
                                (game as GBAgame)
                            )
                            it.putExtra("gametype", Gametype.GBA.name)
                            it.putExtra("cheat", game.GameNum)
                        }

                    "GBC" ->
                        intent.getParcelableExtra<GBgame>("gamedetail").let { game ->
                            it.putExtra(
                                "gamedetail",
                                (game as GBgame)
                            )
                            it.putExtra("gametype", Gametype.GB.name)
                        }

                    else ->
                        intent.getParcelableExtra<GBgame>("gamedetail").let { game ->
                            it.putExtra(
                                "gamedetail",
                                (game as GBgame)
                            )
                            it.putExtra("gametype", Gametype.GB.name)
                        }
                }

            })
        }

        findViewById<TextView>(R.id.savestatetbtn).setOnClickListener {
            PauseGame()
            PopDialogFragment(getString(R.string.savestatetitle))
                .also {
                    it.setOnDialogClickListener(object : OnDialogClickListener {
                        override fun onPostive() {
                            var isSaved = QuickSaveState()
                            Toast.makeText(
                                this@GameActivity,
                                if (isSaved) {
                                    getString(R.string.state_saved)
                                } else
                                    getString(R.string.state_save_fail),
                                Toast.LENGTH_SHORT
                            ).show()
                            ResumeGame()
                        }

                        override fun onNegative() {
                            ResumeGame()
                        }

                        override fun onDismiss() {
                            ResumeGame()
                        }
                    })
                }
                .show(supportFragmentManager, "loadstate")
        }
        findViewById<TextView>(R.id.loadstatebtn).setOnClickListener {
            PauseGame()
            PopDialogFragment(getString(R.string.loadstatetitle))
                .also {
                    it.setOnDialogClickListener(object : OnDialogClickListener {
                        override fun onPostive() {
                            Toast.makeText(
                                this@GameActivity,
                                if (QuickLoadState())
                                    getString(R.string.state_loaded)
                                else getString(R.string.state_load_fail), Toast.LENGTH_SHORT
                            ).show()
                            ResumeGame()
                        }

                        override fun onNegative() {
                            ResumeGame()
                        }

                        override fun onDismiss() {
                            ResumeGame()
                        }
                    })
                }
                .show(supportFragmentManager, "loadstate")
        }
        findViewById<ImageView>(R.id.menubtn).setOnClickListener {
            PauseGame()
            MemorySearchFragment(templateResult).also {
                it.setOnMemSearchListener(object : OnMemSearchListener {
                    override fun onSearch(value: Int) {
                        searchMemory(value)
                        it.updateMemAddressList(templateResult)
                    }

                    override fun onNewSearch(value: Int) {
                        searchMemory(value, true)
                        it.updateMemAddressList(templateResult)
                    }

                    override fun onExit() {
                        it.dismiss()
                        ResumeGame()
                    }
                })
                it.setOnAddressClickListener(object : OnAddressClickListener {
                    override fun onClick(address: Pair<Int, Int>) {
                        val editText = EditText(this@GameActivity)
                        val savetocheattitle = EditText(this@GameActivity)
                        val savetocheatvalue = EditText(this@GameActivity)
                        val layout = LinearLayout(this@GameActivity).also {
                            it.orientation = LinearLayout.VERTICAL
                            it.addView(savetocheattitle)
                            it.addView(savetocheatvalue)
                        }
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@GameActivity)
                        builder.setTitle(
                            getString(
                                R.string.setcheatvalue,
                                address.first.toString(16).padStart(8, '0')
                            )
                        )
                            .setView(editText)
                            .setNegativeButton(getString(R.string.cancel),
                                { dialog, which -> dialog.dismiss() })
                        builder.setPositiveButton(getString(R.string.ok),
                            { dialog, which ->
                                if (editText.text.toString().isNotBlank())
                                    writeMem(address.first, editText.text.toString().toInt())
                            })
                        builder.setNeutralButton(getString(R.string.addtocheatlist)) { dialog, which ->
                            var builder2 = AlertDialog.Builder(this@GameActivity)
                            builder2.setTitle(
                                getString(R.string.savecheattolist)
                            )
                                .setView(layout)
                                .setNegativeButton(getString(R.string.cancel),
                                    { dialog, which -> dialog.dismiss() })
                            builder2.setPositiveButton(getString(R.string.ok),
                                { dialog, which ->
                                    if (!savetocheattitle.text.toString().equals("")
                                        &&
                                        !savetocheatvalue.text.toString().equals("")
                                    ) {
                                        var internalCheatFile =
                                            getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cht"
                                        File(internalCheatFile).let {
                                            if (!it.exists()) {
                                                it.createNewFile()
                                            }
                                            BufferedWriter(FileWriter(it, true)).also {
                                                it.write("# ${savetocheattitle.text.toString()}")
                                                it.newLine()
                                                var cheataddress = address.first.toString(16)
                                                    .padStart(
                                                        8,
                                                        '0'
                                                    ) + ":" + savetocheatvalue.text.toString().toIntOrNull(16)
                                                it.write(cheataddress)
                                                it.close()
                                            }
                                        }

                                    }
                                })
                            builder2.show()
                        }
                        builder.show()
                    }
                })
            }.show(supportFragmentManager, "search")
//          getMemoryBlock().forEach {
//              Log.d("Thememory::",it.toString())
//              it.valuearray.forEach {
//                  if(it.first.toString(16).equals("2000250")) {
//                      Log.d(
//                          "Memory:::",
//                          "address:${it.first.toString(16)} value:${it.second.toString(16)}"
//                      )
//                  }
////                  if(it==9999){
////                      Log.d("getvalue:::",it.toString())
////                  }
//              }
//          }

//            ResumeGame()
//            PauseGame()
//            GameMenuFragment().also {
//                it.setOndismissListener(object : OnMenuListener {
//                    override fun onDismiss() {
//                        ResumeGame()
//                    }
//
//                    override fun onSaveState() {
//                    }
//
//                    override fun onLoadState() {
//                    }
//
//                    override fun onExit() {
//                        System.exit(0)
//                    }
//                })
//            }.show(supportFragmentManager, "menu")
        }

        findViewById<TextView>(R.id.forwardbtn).setOnClickListener {
            setFPS = when (setFPS) {
                60f -> setForward(it as TextView, 2)
                60f * 2 -> setForward(it as TextView, 4)
                else -> {
                    (it as TextView).text = getString(R.string.forward)
                    60f
                }
            }
            Forward(setFPS)
        }
        findViewById<ImageView>(R.id.soundbtn).setOnClickListener {
            Mute(!isMute)
            isMute = !isMute
            when (isMute) {
                false -> (it as ImageView).setImageDrawable(getDrawable(R.drawable.baseline_volume_up_24))
                true -> (it as ImageView).setImageDrawable(getDrawable(R.drawable.baseline_volume_off_24))
            }
//            PauseGame()
//            CheatUtils.memorySearch(999900)
//            ResumeGame()
        }
        findViewById<ImageView>(R.id.rBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.lBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.aBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.bBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.selectBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.startBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.upBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.downBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.leftBtn).setGBAKeyListener()
        findViewById<ImageView>(R.id.rightBtn).setGBAKeyListener()
    }


    private fun searchMemory(value: Int, isNewSearch: Boolean = false) {
        var mem = ArrayList<Pair<Int, Int>>()
        getMemoryBlock().filter {
            it.id == 2.toLong() || it.id == 3.toLong()
        }.forEach {
            mem += it.valuearray
        }
        if (isNewSearch) {
            templateResult = ArrayList(mem.filter {
                it.second == value
            })
        } else {
            templateResult = ArrayList(findMatchingPairs(templateResult, mem).filter {
                it.second == value
            })
        }
    }

    private fun findMatchingPairs(
        a: ArrayList<Pair<Int, Int>>,
        b: ArrayList<Pair<Int, Int>>
    ): ArrayList<Pair<Int, Int>> {
        val set = HashSet<Int>()
        val aFirstValues = a.map { it.first }.toSet()
        val result = b.filter { it.first in aFirstValues } as ArrayList<Pair<Int, Int>>
        return result
    }

    private fun getScreenShot() {
        intent.getStringExtra("gamepath")?.replace(".gba", ".jpg")?.apply {
            var screenshotfile = File(this)
            if (!screenshotfile.exists()) screenshotfile.createNewFile()
            TakeScreenshot(this)
        }
    }

    private fun setForward(view: TextView, times: Int): Float {
        view.text = getString(R.string.forwarding, times.toString())
        return 60f * times
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        var handled = false
        ev?.let {
            getDirectionPressed(ev).let {
                if (it == 0) {
                    lastDirect.forEach {
                        onNativeKeyUp(it)
                        lastDirect.remove(it)
                    }
                    handled = true
                } else {
                    var gbaKey = getKey(it)
                    if (gbaKey != GBAKeys.GBA_KEY_NONE.key) {
                        onNativeKeyDown(gbaKey)
                        handled = true
                    }
                }
            }
        }
        return handled || super.dispatchGenericMotionEvent(ev)
    }

    private fun View.setGBAKeyListener() {
        var keyText = getKey(
            when (this.id) {
                R.id.upBtn -> "up"
                R.id.downBtn -> "down"
                R.id.leftBtn -> "left"
                R.id.rightBtn -> "right"
                R.id.rBtn -> "R"
                R.id.lBtn -> "L"
                R.id.aBtn -> "A"
                R.id.bBtn -> "B"
                R.id.selectBtn -> "select"
                R.id.startBtn -> "start"
                else -> ""
            }
        )
        this.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onNativeKeyDown(keyText)
                }

                MotionEvent.ACTION_UP -> {
                    onNativeKeyUp(keyText)
                }
            }
            true
        }
    }

    //    override fun getArguments(): Array<String> {
//        var gamepath = intent.getStringExtra("gamepath")
//        val gameNum = intent.getStringExtra("cheat")
//        var cheatpath = gamepath?.replace(".gba", ".cheats")
//        if (!File(cheatpath).exists()) cheatpath = null
//        var internalCheatFile = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
//
//        var fragmentShader = "uniform sampler2D tex;\n" +
//                "uniform vec2 texSize;\n" +
//                "varying vec2 texCoord;\n" +
//                "\n" +
//                "uniform float boundBrightness;\n" +
//                "\n" +
//                "void main()\n" +
//                "{\n" +
//                "\tvec4 color = texture2D(tex, texCoord);\n" +
//                "\n" +
//                "\tif (int(mod(texCoord.s * texSize.x * 3.0, 3.0)) == 0 ||\n" +
//                "\t\tint(mod(texCoord.t * texSize.y * 3.0, 3.0)) == 0)\n" +
//                "\t{\n" +
//                "\t\tcolor.rgb *= vec3(1.0, 1.0, 1.0) * boundBrightness;\n" +
//                "\t}\n" +
//                "\n" +
//                "\tgl_FragColor = color;\n" +
//                "}"
//        return if (gamepath != null) {
//            CheatUtils.generateCheat(this, gameNum, cheatpath)
//            arrayOf(
//                gamepath,
//                internalCheatFile,
//                fragmentShader
//            )
//        } else emptyArray<String>()
//
//    }
    external fun reCallCheats(cheatfile: String)
    external fun QuickSaveState(): Boolean
    external fun QuickLoadState(): Boolean
    external fun PauseGame()
    external fun ResumeGame()
    external fun TakeScreenshot(path: String)
    external fun Forward(speed: Float)
    external fun Mute(mute: Boolean)
    external fun getMemoryBlock(): ArrayList<CoreMemoryBlock>
    external fun writeMem(address: Int, value: Int)
    external fun initSwappy()
    external fun setShader(path: String): Boolean
    external fun getFPS(): Float
}


