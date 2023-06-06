package hh.game.mgba_android.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.getStorageId
import com.blankj.utilcode.util.ActivityUtils.startActivity
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import hh.game.mgba_android.GameListViewmodel
import hh.game.mgba_android.R
import hh.game.mgba_android.activity.ui.theme.Mgba_AndroidTheme
import hh.game.mgba_android.database.GB.GBgameData
import hh.game.mgba_android.database.GBA.GBAgameData
import hh.game.mgba_android.mGBAApplication
import hh.game.mgba_android.utils.Gametype

class GameListMaterialActivity : ComponentActivity() {
    private val viewModel: GameListViewmodel by viewModels<GameListViewmodel>()
    private val storageHelper = SimpleStorageHelper(this)
    private var sharepreferences: SharedPreferences? = null
    private var storageid: String? = null
    private var FOLDER_PATH: String = "folder_path"
    private var STORAGEID: String = "storageid"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        var documentfile = DocumentFile.fromTreeUri(this@GameListMaterialActivity, uri)
        var coverfilefolder = documentfile?.findFile("gbacovers")
        viewModel.gameListData.observe(this, { list ->
            setContent {
                Mgba_AndroidTheme {
                    Column {
                        TopbarView()
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            GameList(list.toList(), coverfilefolder)
                        }
                    }
                }
            }
        })
        viewModel.getGbaGameList(this, documentfile)
    }
}

fun onClickgame(game: Any) {
    startActivity(
        Intent(
            mGBAApplication.context,
            GameActivity::class.java
        ).also {
            var gamepath = when (game) {
                is GBAgameData -> game.gbaDocumentFile.getAbsolutePath(mGBAApplication.context)
                else -> (game as GBgameData).gbDocumentFile.getAbsolutePath(
                    mGBAApplication.context
                )
            }
            it.putExtra(
                "gamepath",
                gamepath
            )
            when (game) {
                is GBAgameData -> {
                    it.putExtra("gamedetail", (game as GBAgameData).gbaGame)
                    it.putExtra("gametype", Gametype.GBA.name)
                    it.putExtra("cheat", game.gbaGame.GameNum)

                }

                is GBgameData -> {
                    it.putExtra("gamedetail", (game as GBgameData).gBgame)
                    it.putExtra("gametype", Gametype.GB.name)
                }
            }
        })
}

fun onLongclickGame(game: Any) {
    startActivity(
        Intent(
            mGBAApplication.context,
            CheatsActivity::class.java
        ).also {
            when (game) {
                is GBAgameData -> {
                    it.putExtra("gamedetail", (game as GBAgameData).gbaGame)
                    it.putExtra(
                        "gamepath",
                        game.gbaDocumentFile.getAbsolutePath(mGBAApplication.context)
                    )
                    it.putExtra("gametype", Gametype.GBA.name)
                }

                is GBgameData -> {
                    it.putExtra("gamedetail", (game as GBgameData).gBgame)
                    it.putExtra(
                        "gamepath",
                        game.gbDocumentFile.getAbsolutePath(mGBAApplication.context)
                    )
                    it.putExtra("gametype", Gametype.GB.name)
                }
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopbarView() {
    TopAppBar(
        title = {
            Text(LocalContext.current.getString(R.string.app_name))
        },
        navigationIcon = {
            IconButton(
                onClick = { }
            ) {
            }
        }
    )
}

@Composable
fun GameList(gameList: List<Any>, coverfilefolder: DocumentFile?) {
    LazyColumn {
        items(gameList) { game ->
            if (game is GBAgameData)
                GameRow(game, coverfilefolder, { onClickgame(game) }, { onLongclickGame(game) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun GameRow(
    game: Any,
    coverfilefolder: DocumentFile?,
    onclick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        border = BorderStroke(1.dp, Color.Gray),
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .combinedClickable(
                onClick = onclick,
                onLongClick = onLongClick
            )
    ) {
//        Row(verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .padding(3.dp)
//                ) {
        if (game is GBAgameData) {
            GlideImage(
                model = coverfilefolder?.getAbsolutePath(LocalContext.current) + "/${game.gbaGame.GameNum}.png",
                contentDescription = "image",
                modifier = Modifier.size(100.dp)
            )
        }
        Spacer(modifier = Modifier.width(3.dp))
        Column {
            Text(
                text = if (game is GBAgameData) game.gbaGame.ChiGamename
                    ?: game.gbaDocumentFile.name ?: ""
                else (game as GBgameData).gBgame.EngGamename ?: game.gbDocumentFile.name ?: "",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (game is GBAgameData) game.gbaDocumentFile.getAbsolutePath(LocalContext.current)
                else (game as GBgameData).gbDocumentFile.getAbsolutePath(LocalContext.current),
                style = MaterialTheme.typography.bodySmall
            )
        }
//        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Mgba_AndroidTheme {
    }
}