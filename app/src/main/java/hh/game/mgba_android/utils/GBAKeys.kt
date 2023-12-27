package hh.game.mgba_android.utils

import android.view.KeyEvent

enum class GBAKeys(val key: Int) {
    GBA_KEY_A(KeyEvent.KEYCODE_X),
    GBA_KEY_B(KeyEvent.KEYCODE_Z),
    GBA_KEY_L(KeyEvent.KEYCODE_Q),
    GBA_KEY_R(KeyEvent.KEYCODE_U),
    GBA_KEY_START(KeyEvent.KEYCODE_Y),
    GBA_KEY_SELECT(KeyEvent.KEYCODE_N),
    GBA_KEY_UP(KeyEvent.KEYCODE_DPAD_UP),
    GBA_KEY_DOWN(KeyEvent.KEYCODE_DPAD_DOWN),
    GBA_KEY_LEFT(KeyEvent.KEYCODE_DPAD_LEFT),
    GBA_KEY_RIGHT(KeyEvent.KEYCODE_DPAD_RIGHT),
    GBA_KEY_NONE(-1)
}

fun getKey(text: String): Int =
    (when (text) {
        "A" -> GBAKeys.GBA_KEY_A.key
        "B" -> GBAKeys.GBA_KEY_B.key
        "R" -> GBAKeys.GBA_KEY_R.key
        "L" -> GBAKeys.GBA_KEY_L.key
        "select" -> GBAKeys.GBA_KEY_SELECT.key
        "start" -> GBAKeys.GBA_KEY_START.key
        "up" -> GBAKeys.GBA_KEY_UP.key
        "down" -> GBAKeys.GBA_KEY_DOWN.key
        "left" -> GBAKeys.GBA_KEY_LEFT.key
        "right" -> GBAKeys.GBA_KEY_RIGHT.key
        else -> GBAKeys.GBA_KEY_NONE.key
    })

fun getKey(key: Int): Int =
    when (key) {
        KeyEvent.KEYCODE_BUTTON_A -> GBAKeys.GBA_KEY_A
        KeyEvent.KEYCODE_BUTTON_B -> GBAKeys.GBA_KEY_B
        KeyEvent.KEYCODE_BUTTON_L1 -> GBAKeys.GBA_KEY_L
        KeyEvent.KEYCODE_BUTTON_L2 -> GBAKeys.GBA_KEY_R
        KeyEvent.KEYCODE_BUTTON_SELECT -> GBAKeys.GBA_KEY_SELECT
        KeyEvent.KEYCODE_BUTTON_START -> GBAKeys.GBA_KEY_START
        KeyEvent.KEYCODE_DPAD_UP -> GBAKeys.GBA_KEY_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> GBAKeys.GBA_KEY_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> GBAKeys.GBA_KEY_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> GBAKeys.GBA_KEY_RIGHT
        else -> GBAKeys.GBA_KEY_NONE
    }.key
