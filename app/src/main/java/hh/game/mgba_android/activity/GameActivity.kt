package hh.game.mgba_android.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import hh.game.mgba_android.utils.Gametype
import hh.game.mgba_android.utils.getKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLUtils
import org.libsdl.app.SDLUtils.libraries
import org.libsdl.app.SDLUtils.mFullscreenModeActive
import org.libsdl.app.SDLUtils.mLayout
import org.libsdl.app.SDLUtils.mSurface
import org.libsdl.app.SDLUtils.onNativeKeyDown
import org.libsdl.app.SDLUtils.onNativeKeyUp
import java.io.File
import kotlin.math.roundToInt


class GameActivity : AppCompatActivity() {
    private var surfaceparams: LayoutParams? = null
    private var runFPS = true
    private var setFPS = 60f
    private var isMute = false
    private var templateResult = ArrayList<Pair<Int,Int>>()
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
        SDLUtils.init(this,findViewById(R.id.gameView))
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
    }

    override fun onDestroy() {
        super.onDestroy()
        runFPS = false
//        GlobalScope.cancel()
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        SDLUtils.dispatchKeyEvent(event)

    private fun addGameControler() {
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
                it.setOnMemSearchListener(object:OnMemSearchListener{
                    override fun onSearch(value: Int) {
                        searchMemory(value)
                        it.updateMemAddressList(templateResult)
                    }

                    override fun onNewSearch(value: Int) {
                        searchMemory(value,true)
                        it.updateMemAddressList(templateResult)
                    }

                    override fun onExit() {
                        it.dismiss()
                        ResumeGame()
                    }
                })
                it.setOnAddressClickListener(object:OnAddressClickListener{
                    override fun onClick(address: Pair<Int, Int>) {
                        val editText = EditText(this@GameActivity)
                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@GameActivity)
                        builder.setTitle("Set value to ${address.first.toString(16).padStart(8, '0')}")
                            .setView(editText)
                            .setNegativeButton("Cancel",
                                { dialog, which -> dialog.dismiss() })
                        builder.setPositiveButton("OK",
                            { dialog, which ->
                                writeMem(address.first,editText.text.toString().toInt())
                            })
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

    private fun searchMemory(value:Int,isNewSearch:Boolean = false){
        var mem = ArrayList<Pair<Int,Int>>()
         getMemoryBlock().filter {
            it.id == 2.toLong()||it.id == 3.toLong()
        }.forEach{
             mem+=it.valuearray
         }
        if(isNewSearch) {
            templateResult = ArrayList(mem.filter {
                it.second == value
            })
        }
        else {
            templateResult = ArrayList(findMatchingPairs(templateResult,mem).filter {
                it.second == value
            })
        }
    }
    private fun findMatchingPairs(a: ArrayList<Pair<Int, Int>>, b: ArrayList<Pair<Int, Int>>): ArrayList<Pair<Int, Int>> {
        val set = HashSet<Int>()
        val aFirstValues = a.map { it.first }.toSet()
        val result = b.filter { it.first in aFirstValues } as ArrayList<Pair<Int, Int>>
        return result
    }

    private fun getScreenShot(){
        intent.getStringExtra("gamepath")?.replace(".gba",".jpg")?.apply {
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
    external fun writeMem(address:Int,value: Int)
}


