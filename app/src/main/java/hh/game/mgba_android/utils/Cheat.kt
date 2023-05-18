package hh.game.mgba_android.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cheat (
    var isSelect:Boolean = true,
    var cheatTitle:String="",
    var cheatCode:String=""
): Parcelable{
    override fun toString(): String {
        var disable = if(!isSelect)"!disabled\n" else "!enabled\n"
        return "$disable$cheatTitle\n$cheatCode"
    }
}