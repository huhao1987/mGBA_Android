package hh.game.mgba_android.database.GB

import androidx.documentfile.provider.DocumentFile
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "GBgame", indices = [Index(value = ["Serial"])])
data class GBgame(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "Serial") val Serial: String?,
    @ColumnInfo(name = "EngGamename") val EngGamename: String?,
    @ColumnInfo(name = "crc") val crc: String?,
)
