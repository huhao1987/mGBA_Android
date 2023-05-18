package hh.game.mgba_android.database.GBA

import android.os.Parcelable
import androidx.documentfile.provider.DocumentFile
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "GBAgame", indices = [Index(value = ["Serial"])])
@Parcelize
data class GBAgame(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "Internalname") val Internalname: String?,
    @ColumnInfo(name = "Serial") val Serial: String?,
    @ColumnInfo(name = "EngGamename") val EngGamename: String?,
    @ColumnInfo(name = "ChiGamename") val ChiGamename: String?,
    @ColumnInfo(name = "GameNum") val GameNum: String?,
): Parcelable
