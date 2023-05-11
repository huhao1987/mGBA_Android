package hh.game.mgba_android.database.GBA

import androidx.room.Dao
import androidx.room.Query

@Dao
interface GBAgameDao {
    @Query("SELECT * FROM GBAgame")
    fun getAll(): List<GBAgame>
    @Query("SELECT * FROM GBAgame where Serial like :gameCode || '%'")
    fun getGamelistwithCode(gameCode:String): List<GBAgame>
}