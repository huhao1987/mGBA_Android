package hh.game.mgba_android.database.GB

import androidx.documentfile.provider.DocumentFile
import hh.game.mgba_android.utils.Gametype

data class GBgameData(
    var gbgame: GBgame,
    var gbDocumentFile: DocumentFile,
    var gametype: Gametype = Gametype.GB
)
