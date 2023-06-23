package hh.game.mgba_android.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import hh.game.mgba_android.fragment.GameMenuFragment
import hh.game.mgba_android.fragment.OnMenuListener
import hh.game.mgba_android.R
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.fragment.OnDialogClickListener
import hh.game.mgba_android.fragment.PopDialogFragment
import hh.game.mgba_android.utils.GBAcheatUtils
import hh.game.mgba_android.utils.Gametype
import hh.game.mgba_android.utils.VideoUtils.Companion.captureScreenshot
import hh.game.mgba_android.utils.VideoUtils.Companion.saveScreenshotFile
import hh.game.mgba_android.utils.getKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import org.libsdl.app.SDLActivity
import java.io.File
import kotlin.math.roundToInt


class GameActivity : SDLActivity() {
    private var surfaceparams: LayoutParams? = null
    private var runFPS = true
    private var setFPS = 60f
    private var isMute = false

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
        Log.d("GameActivity::", "create")
        updateScreenPosition()
        addGameControler()
//        GlobalScope.launch {
//            Gameutils.getFPS().toString()
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runFPS = false
        GlobalScope.cancel()
    }

    fun updateScreenPosition() {
        if (surfaceparams == null) {
            surfaceparams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            val screenWidth = windowManager.defaultDisplay.width
            surfaceparams?.topMargin = 0
            surfaceparams?.leftMargin = 0
            surfaceparams?.width = screenWidth
            surfaceparams?.height = (screenWidth * 0.7).roundToInt()
            mSurface.layoutParams = surfaceparams
        }
    }

    private fun addGameControler() {
        val inflater = LayoutInflater.from(this)
        val relativeLayout =
            inflater.inflate(R.layout.padboard, mLayout, false) as RelativeLayout
        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        layoutParams.bottomMargin = 30.dpToPx()
        layoutParams.leftMargin = 10.dpToPx()
        layoutParams.rightMargin = 10.dpToPx()
        mLayout.addView(relativeLayout, layoutParams)
        relativeLayout.findViewById<TextView>(R.id.cheatbtn).setOnClickListener {
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

        relativeLayout.findViewById<TextView>(R.id.savestatetbtn).setOnClickListener {
            PauseGame()
            captureScreenshot(mSurface) { bitmap: Bitmap? ->
                saveScreenshotFile(this, bitmap)
            }


            PopDialogFragment(getString(R.string.savestatetitle))
                .also {
                    it.setOnDialogClickListener(object : OnDialogClickListener {
                        override fun onPostive() {
                            Toast.makeText(
                                this@GameActivity,
                                if (QuickSaveState()) {
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
        relativeLayout.findViewById<TextView>(R.id.loadstatebtn).setOnClickListener {
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
        relativeLayout.findViewById<ImageView>(R.id.menubtn).setOnClickListener {
            PauseGame()
            GameMenuFragment().also {
                it.setOndismissListener(object : OnMenuListener {
                    override fun onDismiss() {
                        ResumeGame()
                    }

                    override fun onSaveState() {
                    }

                    override fun onLoadState() {
                    }

                    override fun onExit() {
                        System.exit(0)
                    }
                })
            }.show(supportFragmentManager, "menu")
        }

        relativeLayout.findViewById<TextView>(R.id.forwardbtn).setOnClickListener {
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
        relativeLayout.findViewById<ImageView>(R.id.soundbtn).setOnClickListener {
            Mute(!isMute)
            isMute = !isMute
            when (isMute) {
                false -> (it as ImageView).setImageDrawable(getDrawable(R.drawable.baseline_volume_up_24))
                true -> (it as ImageView).setImageDrawable(getDrawable(R.drawable.baseline_volume_off_24))
            }
        }
        relativeLayout.findViewById<ImageView>(R.id.rBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.lBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.aBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.bBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.selectBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.startBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.upBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.downBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.leftBtn).setGBAKeyListener()
        relativeLayout.findViewById<ImageView>(R.id.rightBtn).setGBAKeyListener()
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


    override fun getArguments(): Array<String> {
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
        return if (gamepath != null) {
         GBAcheatUtils.generateCheat(this, gameNum, cheatpath)
                arrayOf(
                    gamepath,
                    internalCheatFile,
                    fragmentShader
                )
        } else emptyArray<String>()

    }

    override fun resumeNativeThread() {
        super.resumeNativeThread()
    }

    external fun reCallCheats(cheatfile: String)
    external fun QuickSaveState(): Boolean
    external fun QuickLoadState(): Boolean
    external fun PauseGame()
    external fun ResumeGame()
    external fun TakeScreenshot(): ByteArray
    external fun Forward(speed: Float)
    external fun Mute(mute: Boolean)
}


