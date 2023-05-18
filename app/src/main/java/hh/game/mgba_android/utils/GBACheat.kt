package hh.game.mgba_android.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GBACheat(
    var gameTitle: String = "",
    var gameSystem: Gametype = Gametype.GBA,
    var gameDes: String = "",
    var cheatlist: ArrayList<Cheat>? = ArrayList<Cheat>()
) : Parcelable {
    override fun toString(): String {
        return "${
            cheatlist?.joinToString(
                separator = " ",
                prefix = "",
                postfix = ""
            )
        }".replace(",", "").replace("[", "").replace("]", "")
    }
}



