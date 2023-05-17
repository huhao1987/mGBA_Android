package hh.game.mgba_android.activity

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams
import com.blankj.utilcode.util.FileIOUtils
import hh.game.mgba_android.R
import hh.game.mgba_android.utils.GBAcheatUtils
import hh.game.mgba_android.utils.getKey
import org.libsdl.app.SDLActivity
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt


class GameActivity : SDLActivity() {
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
        var internalCheatFile = getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats"
        if (!File(internalCheatFile).exists()) {
            try {
                var cheatfromasset = this.assets.open("gbacheats/$gameNum.cht")
                var cheat =
                    GBAcheatUtils().convertECcodestoVba(cheatfromasset)
                        .toString()
                FileIOUtils.writeFileFromString(
                    getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats",
                    cheat
                )
                Log.d("thecheat:::", cheat)
            } catch (e: IOException) {

            }

        }
        return if (gamepath != null) {
            if (File(internalCheatFile).exists())
                arrayOf(
                    gamepath,
                    internalCheatFile
                )
            else arrayOf(
                gamepath
            )
        } else emptyArray<String>()

    }
}


