package hh.game.mgba_android

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.file.getAbsolutePath
import hh.game.mgba_android.database.GB.GBgameData
import hh.game.mgba_android.database.GBA.GBAgameData
import hh.game.mgba_android.utils.GameListListener
import hh.game.mgba_android.utils.Gameutils
import kotlinx.coroutines.launch

class GameListViewmodel : ViewModel() {
    var gameListData = MutableLiveData<ArrayList<Any>>()
    fun getGbaGameList(context: Context, documentfile: DocumentFile?) {
        viewModelScope.launch {
            documentfile?.apply {
                var gamelist = ArrayList(documentfile?.listFiles()?.filter {
                    it.getAbsolutePath(context).contains(".gba", ignoreCase = true)
                            ||
                            it.getAbsolutePath(context).contains(".gb", ignoreCase = true)
                }?.toList())
                Gameutils.getGameList(
                    context,
                    gamelist,
                    object : GameListListener {
                        override fun onGetGamelist(
                            gbagamelist: ArrayList<GBAgameData>,
                            gbgamelist: ArrayList<GBgameData>
                        ) {
                            gameListData.postValue(ArrayList(gbagamelist + gbgamelist))
                        }
                    })
            }
        }

    }
}