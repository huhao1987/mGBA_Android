package hh.game.mgba_android.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.getStorageId
import hh.game.mgba_android.adapter.GameListAdapter
import hh.game.mgba_android.R
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.utils.Gameutils
import hh.game.mgba_android.utils.GameDetailsListener
import hh.game.mgba_android.utils.Gametype
import kotlinx.coroutines.runBlocking

class GameListActivity : AppCompatActivity() {
    private val storageHelper = SimpleStorageHelper(this)
    private var sharepreferences: SharedPreferences? = null
    private var storageid: String? = null
    private var FOLDER_PATH: String = "folder_path"
    private var STORAGEID: String = "storageid"
    private lateinit var gameListAdapter: GameListAdapter
    private lateinit var gamelistview: RecyclerView
    private var gamelist: ArrayList<DocumentFile>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_list)
        gamelistview = findViewById<RecyclerView>(R.id.gamelistview)
        gameListAdapter = GameListAdapter(this, ArrayList())
        sharepreferences = getSharedPreferences("mGBA", Context.MODE_PRIVATE)
        var permissionlist = contentResolver.persistedUriPermissions

        if (permissionlist.size > 0) {
            storageid = sharepreferences?.getString(STORAGEID, null)
            setupUI()
        } else {
            sharepreferences?.edit()?.putString(FOLDER_PATH, null)?.apply()
            storageHelper.openFolderPicker()
            setupStorageFolder()
        }
    }

    fun setupStorageFolder() {
        storageHelper.onFolderSelected = { requestCode, folder ->
            sharepreferences?.edit()?.putString(FOLDER_PATH, folder.uri.toString())?.apply()
            storageid = folder.getStorageId(this)
            sharepreferences?.edit()?.putString(STORAGEID, storageid)?.apply()
            setupUI()
        }
    }

    fun setupUI() {
        var uri = Uri.parse(sharepreferences?.getString(FOLDER_PATH, null))
        var documentfile = DocumentFile.fromTreeUri(this, uri)
        gamelistview.layoutManager = LinearLayoutManager(this)
        gamelistview.adapter = gameListAdapter.also {
            gamelist = ArrayList(documentfile?.listFiles()?.filter {
                it.getAbsolutePath(this).contains(".gba", ignoreCase = true)
                        ||
                        it.getAbsolutePath(this).contains(".gb", ignoreCase = true)
            }?.toList())
            gamelist?.apply {
                it.updateList(this)
                it.itemClickListener = { position ->
                    startActivity(Intent(this@GameListActivity, GameActivity::class.java).also {
                        it.putExtra(
                            "gamepatch",
                            this.get(position).getAbsolutePath(this@GameListActivity)
                        )
                    })
                }
            }

        }
    }
}