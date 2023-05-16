package hh.game.mgba_android.database.GBA

import androidx.documentfile.provider.DocumentFile
import hh.game.mgba_android.utils.Gametype

data class GBAgameData(
    var gbaGame: GBAgame,
    var gbaDocumentFile: DocumentFile,
    var gametype: Gametype = Gametype.GBA
)
