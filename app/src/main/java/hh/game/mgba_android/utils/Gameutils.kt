package hh.game.mgba_android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.getAbsolutePath
import hh.game.mgba_android.database.GB.GBgameData
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.database.GBA.GBAgameData
import hh.game.mgba_android.database.GameDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class Gameutils(var context: Context, var path: String) {
    companion object {
        init {
            try {
                System.loadLibrary("oboe")
            } catch (e: UnsatisfiedLinkError) {
                // Fallback or log if needed, but usually it should be present
            }
            System.loadLibrary("mgba_android")
        }

        fun getGameList(
            context: Context,
            list: ArrayList<DocumentFile>,
            gameListListener: GameListListener
        ) {
            runBlocking {
                withContext(Dispatchers.IO) {
                    var gbaList = ArrayList<GBAgameData>()
                    var gbList = ArrayList<GBgameData>()
                    list.forEach {
                        if (it.name!!.contains(".gba", true)) {
                            try {
                                GameDatabase.getInstance(context).gbagameDao()
                                    .getGamelistwithCode(
                                        Gameutils(
                                            context,
                                            it.getAbsolutePath(context)
                                        ).init().getGameCode()
                                    )[0].apply {
                                    gbaList.add(GBAgameData(this, it))
                                }
                            } catch (e: Exception) {
                                gbaList.add(
                                    GBAgameData(
                                        GBAgame(
                                            uid = 9999,
                                            GameNum = "",
                                            Internalname = "",
                                            Serial = "",
                                            EngGamename = it.name?.replace(".gba", ""),
                                            ChiGamename = it.name?.replace(".gba", "")
                                        ), it
                                    )
                                )
                            }
                        } else {
                            gbList.add(
                                GBgameData(
                                    GameDatabase.getInstance(context).gbgameDao()
                                        .getGamelistwithCode(
                                            Gameutils(
                                                context,
                                                it.getAbsolutePath(context)
                                            ).init().getGameCode()
                                        )[0], it
                                )
                            )
                        }
                    }
                    gameListListener.onGetGamelist(
                        gbaList, gbList
                    )
                }
            }
        }

        @Throws(IOException::class)
        suspend fun readGuidePic(zipfilepath: String,imgname:String): ImageBitmap? {
            return withContext(Dispatchers.IO) {
            ZipFile(zipfilepath).use { zf ->
                BufferedInputStream(FileInputStream(zipfilepath)).use { inStream ->
                    ZipInputStream(inStream).use { zin ->
                        var ze: ZipEntry?
                        while (zin.nextEntry.also { ze = it } != null) {
                            if (!ze!!.isDirectory) {
                                Log.i("tag", "file - " + ze!!.name + " : " + ze!!.size + " bytes")
                                if (ze!!.name == "gbacovers/"+imgname) {
                                    zf.getInputStream(ze!!).use { isStream ->
                                         BitmapFactory.decodeStream(isStream).asImageBitmap()
                                    }
                                }
                            }
                        }
                    }
                }
            }
             null}
        }

        suspend fun loadImageFromZip(zipPath: String?, imagePath: String?): ImageBitmap? {
            if(zipPath == null || imagePath == null) return null
            return withContext(Dispatchers.IO) {
                val zipFile = ZipFile(zipPath)
                val inputStream = zipFile.getInputStream(zipFile.getEntry(imagePath))
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap.asImageBitmap()
            }
        }
        external fun getFPS(): Float
    }

    fun init(): Gameutils {
        initCore(path)
        return this
    }

    suspend fun loadGames(
        gametype: Gametype?,
        gameDetailsListener: GameDetailsListener
    ) {
        coroutineScope {
            launch {
                var gamecode = getGameCode()
                var game = withContext(Dispatchers.IO) {
                    if (gamecode != null && gametype != null)
                        when (gametype) {
                            Gametype.GBA ->
                                GameDatabase.getInstance(context).gbagameDao()
                                    .getGamelistwithCode(gamecode)
                            Gametype.GBC -> {
                                GameDatabase.getInstance(context).gbgameDao()
                                    .getGamelistwithCode(gamecode)
                            }
                            Gametype.GB -> {
                                GameDatabase.getInstance(context).gbgameDao()
                                    .getGamelistwithCode(gamecode)
                            }
                        }.get(0)
                    else null
                }
                gameDetailsListener.onGetDetails(game)
            }
        }
    }

    external fun initCore(path: String)
    external fun getGameTitle(): String
    external fun getGameCode(): String

}

interface GameDetailsListener {
    fun onGetDetails(gameDetails: Any?)
}

interface GameListListener {
    fun onGetGamelist(gbagamelist: ArrayList<GBAgameData>, gbgamelist: ArrayList<GBgameData>)
}