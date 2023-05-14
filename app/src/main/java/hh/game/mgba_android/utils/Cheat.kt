package hh.game.mgba_android.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cheat (
    var cheatTitle:String="",
    var cheatCode:String=""
): Parcelable{
    override fun toString(): String {
        return "$cheatTitle\n$cheatCode"
    }
}