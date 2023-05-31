package hh.game.mgba_android.activity

import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
import hh.game.mgba_android.GameMenuFragment
import hh.game.mgba_android.OnMenuListener
import hh.game.mgba_android.R
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.utils.GBAcheatUtils
import hh.game.mgba_android.utils.Gametype
import hh.game.mgba_android.utils.getKey
import org.libsdl.app.SDLActivity
import java.io.File
import kotlin.math.roundToInt


class GameActivity : SDLActivity() {
    private val requestcode = 123
    private var surfaceparams: LayoutParams? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFullscreenModeActive = false
        Log.d("GameActivity::", "create")
        updateScreenPosition()
        addGameControler()
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
            startActivityForResult(Intent(this,CheatsActivity::class.java).also {
                when(intent.getStringExtra("gametype")){
                    "GBA" ->
                        intent.getParcelableExtra<GBAgame>("gamedetail").let {
                            game ->
                            it.putExtra(
                                "gamedetail",
                                (game as GBAgame)
                            )
                            it.putExtra("gametype", Gametype.GBA.name)
                            it.putExtra("cheat", game.GameNum)
                        }

                    else ->
                        intent.getParcelableExtra<GBgame>("gamedetail").let {
                                game ->
                            it.putExtra(
                                "gamedetail",
                                (game as GBgame)
                            )
                            it.putExtra("gametype", Gametype.GB.name)
                        }
                }

            },requestcode)
        }

        relativeLayout.findViewById<TextView>(R.id.savestatetbtn).setOnClickListener {
             let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setTitle(R.string.savestatetitle)
                    setPositiveButton(R.string.ok,
                        DialogInterface.OnClickListener { dialog, id ->
                            SaveState()
                            dialog.dismiss()
                        })
                    setNegativeButton(R.string.cancel,
                        DialogInterface.OnClickListener { dialog, id ->
                            dialog.dismiss()
                        })
                }
                builder.create()
            }.show()
        }
        relativeLayout.findViewById<TextView>(R.id.loadstatebtn).setOnClickListener {
            let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setTitle(R.string.loadstatetitle)
                    setPositiveButton(R.string.ok,
                        DialogInterface.OnClickListener { dialog, id ->
                            LoadState()
                            dialog.dismiss()
                        })
                    setNegativeButton(R.string.cancel,
                        DialogInterface.OnClickListener { dialog, id ->
                            dialog.dismiss()
                        })
                }
                builder.create()
            }.show()
        }
        relativeLayout.findViewById<TextView>(R.id.menubtn).setOnClickListener {
            PauseGame()
            GameMenuFragment().also {
                it.setOndismissListener(object:OnMenuListener{
                    override fun onDismiss() {
                        ResumeGame()
                    }
                    override fun onSaveState() {
                    }
                    override fun onLoadState() {
                    }
                    override fun onExit() {
                        onBackPressed()
                        finish()
                    }
                })
            }.show(supportFragmentManager, "menu")
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
        var cheatpath = gamepath?.replace(".gba",".cheats")
        if(!File(cheatpath).exists()) cheatpath = null
        var internalCheatFile = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
        return if (gamepath != null) {
            if (GBAcheatUtils.generateCheat(this,gameNum,cheatpath))
                arrayOf(
                    gamepath,
                    internalCheatFile
                )
            else arrayOf(
                gamepath
            )
        } else emptyArray<String>()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            requestCode -> if(resultCode == RESULT_OK){
                val gameNum = intent.getStringExtra("cheat")
                var internalCheatFile = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
                reCallCheats(internalCheatFile)
            }
        }
    }
    override fun resumeNativeThread() {
        super.resumeNativeThread()
    }
    external fun reCallCheats(cheatfile:String)
    external fun SaveState()
    external fun LoadState()
    external fun PauseGame()
    external fun ResumeGame()
}


