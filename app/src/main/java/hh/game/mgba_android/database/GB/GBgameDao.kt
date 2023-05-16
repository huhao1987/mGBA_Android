package hh.game.mgba_android.database.GB

import androidx.room.Dao
import androidx.room.Query

@Dao
interface GBgameDao {
    @Query("SELECT * FROM GBgame")
    fun getAll():List<GBgame>

    @Query("SELECT * FROM GBgame where Serial like :gameCode || '%'")
    fun getGamelistwithCode(gameCode:String): List<GBgame>

}