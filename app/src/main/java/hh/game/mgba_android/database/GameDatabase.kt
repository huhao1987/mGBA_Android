package hh.game.mgba_android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GB.GBgameDao
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.database.GBA.GBAgameDao

@Database(entities = [GBAgame::class, GBgame::class], version = 1)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gbagameDao(): GBAgameDao
    abstract fun gbgameDao(): GBgameDao

    companion object{
        lateinit var db : GameDatabase
        fun getInstance(context: Context):GameDatabase{
            db = Room.databaseBuilder(context ,GameDatabase::class.java,"GBAGameList")
                .createFromAsset("gameList.db")
                .build()
            return db
        }


    }
}