package hh.game.mgba_android.database.GB

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GBgame")
data class GBgame(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "Serial") val Serial: String?,
    @ColumnInfo(name = "EngGamename") val EngGamename: String?,
    @ColumnInfo(name = "crc") val crc: String?,
    )
