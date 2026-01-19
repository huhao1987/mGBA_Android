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

open class GameActivity : AppCompatActivity() {
    private var runFPS = true
    private var setFPS = 60f
    private var isMute = false
    private var templateResult = ArrayList<Pair<Int, Int>>()
    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val gameNum = intent.getStringExtra("cheat")
                val gamePath = intent.getStringExtra("gamepath")
                
                // Priority Load: Game Directory -> Private Directory
                var cheatFileToLoad = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
                if (gamePath != null) {
                    val gameDirCheat = File(File(gamePath).parent, "$gameNum.cheats")
                    // Check .cheats
                    if (gameDirCheat.exists()) {
                         cheatFileToLoad = gameDirCheat.absolutePath
                    } else {
                         // Check .cht
                         val gameDirCht = File(File(gamePath).parent, "$gameNum.cht")
                         if (gameDirCht.exists()) {
                             cheatFileToLoad = gameDirCht.absolutePath
                         }
                    }
                }
                Log.d("GameActivity", "Reloading cheats from: $cheatFileToLoad")
                reCallCheats(processCheatsAndGetNativePath(cheatFileToLoad))
            }
        }

    // Helper to convert Legacy Format (User preferred) to Libretro Format (Native required) on the fly
    private fun isARDSCheat(cheat: hh.game.mgba_android.utils.Cheat): Boolean {
        // Simple Heuristic: Check for AR DS exclusive opcodes in the code lines
        // AR DS Opcodes: D0-DF (Data/Flow), E0 (Patch)
        // Also 30-6F are conditional but we'll focus on the structural ones first to avoid false positives with GBA addresses
        // But the user specifically mentioned "AR DS"
        val lines = cheat.cheatCode.lines()
        for (line in lines) {
            val parts = line.trim().split(" ")
            if (parts.size == 2 && parts[0].length == 8 && parts[1].length == 8) {
               val opStart = parts[0][0].toUpperCase()
               if (opStart == 'D' || opStart == 'E') {
                   return true
               }
            }
        }
        return false
    }

    private fun processCheatsAndGetNativePath(path: String): String {
         val legacyFile = File(path)
         if (!legacyFile.exists()) return path
         
         // Parse using the CheatUtils
         val cheats = CheatUtils.parseUserCheatFile(legacyFile)
         
         // Reset Native AR DS Engine First
         resetARDSCheats()
         var ardsCount = 0
         
         // Generate Libretro content manually for Standard Cheats
         val sb = StringBuilder()
         var stdCount = 0
         
         cheats.forEachIndexed { i, cheat ->
             if (cheat.isSelect) {
                 if (isARDSCheat(cheat)) {
                     // Route to AR DS Engine
                     cheat.cheatCode.lines().forEach { line ->
                         val parts = line.trim().split(" ")
                         if (parts.size >= 2) {
                             try {
                                  val op = parts[0].toLong(16).toInt()
                                  val value = parts[1].toLong(16).toInt()
                                  addARDSCheat(op, value)
                             } catch (e: Exception) {
                                 Log.e("GameActivity", "Error parsing ARDS code: $line")
                             }
                         }
                     }
                     ardsCount++
                     Log.d("GameActivity", "Loaded AR DS Cheat: ${cheat.cheatTitle}")
                 } else {
                     // Route to Standard Engine
                     sb.append("cheat${stdCount}_desc = \"${cheat.cheatTitle}\"\n")
                     val code = cheat.cheatCode.replace("\n", "+")
                     sb.append("cheat${stdCount}_code = \"$code\"\n")
                     sb.append("cheat${stdCount}_enable = true\n\n")
                     stdCount++
                 }
             }
         }
         
         // Add header count for standard cheats
         val finalContent = "cheats = $stdCount\n\n" + sb.toString()
         
         if (ardsCount > 0) {
             Log.d("GameActivity", "Total AR DS Cheats Loaded: $ardsCount")
         }
         
         // Save to a temp location that native core will read
         val nativeFile = File(cacheDir, "running_cheats.cht")
         try {
             val writer = java.io.BufferedWriter(java.io.FileWriter(nativeFile))
             writer.write(finalContent)
             writer.close()
             return nativeFile.absolutePath
         } catch (e: Exception) {
             e.printStackTrace()
             return path // Fallback
         }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFullscreenModeActive = false
        setContentView(R.layout.activity_game)
        var gamepath = intent.getStringExtra("gamepath")
        val gameNum = intent.getStringExtra("cheat")
        Log.d("GameActivity", "onCreate: gameNum='$gameNum', gamepath='$gamepath'")
        
        // Initial Cheat Load Logic (Unified)
        var cheatRefPath = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
        if (gamepath != null) {
             val gameDirCheat = File(File(gamepath).parent, "$gameNum.cheats")
             if (gameDirCheat.exists()) cheatRefPath = gameDirCheat.absolutePath
        }

        // Generate default if missing
        if (gamepath != null)
             CheatUtils.generateCheat(this, gameNum, null)

        var internalCheatFile = processCheatsAndGetNativePath(cheatRefPath)
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
        // Initialize Tools Button
        findViewById<View>(R.id.tools_btn).setOnClickListener {
            val options = arrayOf("Shaders", "Memory Tools")
            AlertDialog.Builder(this)
                .setTitle("Tools")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showShaderMenu()
                        1 -> openHexEditor()
                    }
                }
                .show()
        }

        val fpsText = findViewById<TextView>(R.id.fps_text)
        lifecycleScope.launch(Dispatchers.Main) {
            while (runFPS) {
                fpsText.text = "FPS: %.1f".format(getFPS())
                delay(500)
            }
        }
    }

    private fun showShaderMenu() {
        val shaderDir = File(filesDir, "shaders")
        if (!shaderDir.exists()) shaderDir.mkdirs()
        
        val shaderFiles = shaderDir.listFiles { file -> 
            file.isDirectory || file.extension == "shader" 
        }?.map { it.name }?.toMutableList() ?: mutableListOf()
        
        shaderFiles.add(0, "Clear")
        
        AlertDialog.Builder(this)
            .setTitle("Select Shader")
            .setItems(shaderFiles.toTypedArray()) { _, which ->
                val selectedName = shaderFiles[which]
                if (selectedName == "Clear") {
                    setShader("")
                    // Toast.makeText(this, "Shader Cleared", Toast.LENGTH_SHORT).show()
                } else {
                    val selectedFile = File(shaderDir, selectedName)
                    setShader(selectedFile.absolutePath)
                    // Toast.makeText(this, "Applied: $selectedName", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openHexEditor() {
        PauseGame()
        hh.game.mgba_android.fragment.HexEditorFragment().apply {
            // Dismiss listener handles ResumeGame
        }
        .show(supportFragmentManager, "hex_editor")
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
        // Reload cheats on resume in case they were edited in CheatsActivity
         if (intent.getStringExtra("gamepath") != null) {
            val gamePath = intent.getStringExtra("gamepath")
            val parentDir = File(gamePath).parentFile
            // Use the same gameNum logic as onCreate
            val gameNum = intent.getStringExtra("cheat") ?: File(gamePath).nameWithoutExtension
            
            // Unified Cheat Reloading
            var cheatRefPath = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
            val gameDirCheat = File(parentDir, "$gameNum.cheats") // Priority to game dir
            if (gameDirCheat.exists()) cheatRefPath = gameDirCheat.absolutePath
            
            // Process (Reload AR DS, Update .cht file)
            // Note: Updated .cht file might not be re-read by core without restart, 
            // but AR DS cheats are updated immediately via JNI.
            processCheatsAndGetNativePath(cheatRefPath)
         }
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
                // Pass gamepath so CheatsActivity knows where to look
                it.putExtra("gamepath", intent.getStringExtra("gamepath"))

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
    external fun writeMem8(address: Int, value: Int)
    external fun initSwappy()
    external fun setShader(path: String): Boolean
    external fun getFPS(): Float
    external fun getMemoryRange(address: Int, length: Int): ByteArray?
    external fun nativeMemorySearch(value: Int, size: Int): IntArray?
    external fun resetARDSCheats()
    external fun addARDSCheat(op: Int, valVal: Int)
}


