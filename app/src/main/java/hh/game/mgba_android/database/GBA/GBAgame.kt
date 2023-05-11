package hh.game.mgba_android.database.GBA

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GBAgame")
data class GBAgame(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "Internalname") val Internalname: String?,
    @ColumnInfo(name = "Serial") val Serial: String?,
    @ColumnInfo(name = "EngGamename") val EngGamename: String?,
    @ColumnInfo(name = "ChiGamename") val ChiGamename: String?
)
