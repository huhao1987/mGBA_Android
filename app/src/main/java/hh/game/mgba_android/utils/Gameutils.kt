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
import hh.game.mgba_android.database.GB.GBgame
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
                            var gameCode = ""
                            try {
                                val utils = Gameutils(context, it.getAbsolutePath(context)).init()
                                gameCode = utils.getGameCode()
                                Log.d("Gameutils", "Parsed GameCode: '$gameCode' for file: ${it.name}")
                                
                                val dbGames = GameDatabase.getInstance(context).gbagameDao()
                                    .getGamelistwithCode(gameCode)
                                
                                if (dbGames.isNotEmpty()) {
                                    val dbGame = dbGames[0]
                                    // Override GameNum with our mapping to ensure consistency with cheats.json
                                    val mappedId = CheatDatabaseUtils.getGameIdForSerial(context, gameCode)
                                    val fixedGame = if (mappedId != null) dbGame.copy(GameNum = mappedId) else dbGame
                                    gbaList.add(GBAgameData(fixedGame, it))
                                } else {
                                    // Fallback when not found in internal DB
                                    throw Exception("Game not found in DB")
                                }
                            } catch (e: Exception) {
                                // Expected fallback for unknown games
                                val mappedId = CheatDatabaseUtils.getGameIdForSerial(context, gameCode)
                                val finalGameNum = mappedId ?: gameCode
                                val cheatGameName = CheatDatabaseUtils.getGameName(context, finalGameNum)
                                
                                gbaList.add(
                                    GBAgameData(
                                        GBAgame(
                                            uid = 9999,
                                            GameNum = finalGameNum,
                                            Internalname = "",
                                            Serial = gameCode,
                                            EngGamename = cheatGameName ?: it.name?.replace(".gba", ""),
                                            ChiGamename = cheatGameName ?: it.name?.replace(".gba", "")
                                        ), it
                                    )
                                )
                            }
                        } else {
                            try {
                                val utils = Gameutils(context, it.getAbsolutePath(context)).init()
                                val gameCode = utils.getGameCode()
                                val dbGames = GameDatabase.getInstance(context).gbgameDao().getGamelistwithCode(gameCode)
                                
                                if (dbGames.isNotEmpty()) {
                                    gbList.add(GBgameData(dbGames[0], it))
                                } else {
                                     // Create dummy GB entry
                                     gbList.add(
                                        GBgameData(
                                            GBgame(
                                                uid = 9999,
                                                Serial = gameCode,
                                                EngGamename = it.name?.replace(".gbc", "")?.replace(".gb", ""),
                                                crc = ""
                                            ), it
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("Gameutils", "Error processing GB game: ${it.name}", e)
                                // Minimal fallback to prevent loss
                                gbList.add(
                                    GBgameData(
                                        GBgame(
                                            uid = 9999,
                                            Serial = "",
                                            EngGamename = it.name,
                                            crc = ""
                                        ), it
                                    )
                                )
                            }
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