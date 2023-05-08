package hh.game.mgba_android

import android.view.KeyEvent

enum class GBAKeys(val key:Int) {
    GBA_KEY_A(KeyEvent.KEYCODE_X),
    GBA_KEY_B(KeyEvent.KEYCODE_Z),
    GBA_KEY_L(KeyEvent.KEYCODE_Q),
    GBA_KEY_R(KeyEvent.KEYCODE_U),
    GBA_KEY_START(KeyEvent.KEYCODE_Y),
    GBA_KEY_SELECT(KeyEvent.KEYCODE_N),
    GBA_KEY_UP(KeyEvent.KEYCODE_W),
    GBA_KEY_DOWN(KeyEvent.KEYCODE_S),
    GBA_KEY_LEFT(KeyEvent.KEYCODE_A),
    GBA_KEY_RIGHT(KeyEvent.KEYCODE_D),
    GBA_KEY_NONE(-1)
}

fun getKey(text:String):Int =
    (when (text) {
        "A" -> GBAKeys.GBA_KEY_A.key
        "B" -> GBAKeys.GBA_KEY_B.key
        "R" -> GBAKeys.GBA_KEY_R.key
        "L" -> GBAKeys.GBA_KEY_L.key
        "select" -> GBAKeys.GBA_KEY_SELECT.key
        "start" ->GBAKeys.GBA_KEY_START.key
        "up" -> GBAKeys.GBA_KEY_UP.key
        "down" -> GBAKeys.GBA_KEY_DOWN.key
        "left" -> GBAKeys.GBA_KEY_LEFT.key
        "right" -> GBAKeys.GBA_KEY_RIGHT.key
        else -> GBAKeys.GBA_KEY_NONE.key
    })
