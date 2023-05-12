package hh.game.mgba_android.utils

import android.content.Context
import hh.game.mgba_android.database.GameDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Gameutils(var context: Context, var path: String) {
    companion object {
        init {
            System.loadLibrary("mgba_android")
        }
        external fun getFPS():Int
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
                        if (gamecode != null&&gametype!=null)
                            when(gametype){
                                Gametype.GBA ->
                                    GameDatabase.getInstance(context).gbagameDao()
                                        .getGamelistwithCode(gamecode)
                                Gametype.GB ->{
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