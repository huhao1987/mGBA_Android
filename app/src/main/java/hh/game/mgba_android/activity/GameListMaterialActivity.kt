package hh.game.mgba_android.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.baseName
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.getStorageId
import com.blankj.utilcode.util.ActivityUtils.startActivity
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import hh.game.mgba_android.GameListViewmodel
import hh.game.mgba_android.R
import hh.game.mgba_android.activity.ui.theme.Mgba_AndroidTheme
import hh.game.mgba_android.database.GB.GBgameData
import hh.game.mgba_android.database.GBA.GBAgameData
import hh.game.mgba_android.mGBAApplication
import hh.game.mgba_android.utils.Gametype
import java.io.File

class GameListMaterialActivity : ComponentActivity() {
    private val viewModel: GameListViewmodel by viewModels<GameListViewmodel>()
    private val storageHelper = SimpleStorageHelper(this)
    private var sharepreferences: SharedPreferences? = null
    private var storageid: String? = null
    private var FOLDER_PATH: String = "folder_path"
    private var STORAGEID: String = "storageid"
    val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            checkPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    var intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + packageName)
                    )
                    startForResult.launch(intent)
                } catch (e: Exception) {
                    var intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startForResult.launch(intent)
                }
            } else {
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
        setContent {
            CustomCircularProgressBar()
        }
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
                    it.putExtra("gamedetail", (game as GBgameData).gbgame)
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
                    it.putExtra("gamedetail", (game as GBgameData).gbgame)
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
            GameRow(game, coverfilefolder, { onClickgame(game) }, { onLongclickGame(game) })
        }
    }
}

@Composable
private fun CustomCircularProgressBar() {
    var editable by remember { mutableStateOf(true) }
    AnimatedVisibility(visible = editable) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(size = 64.dp),
                color = Color.Magenta,
                strokeWidth = 6.dp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(230.dp)
                .background(Color.DarkGray)
        ) {
            var filepath =
                coverfilefolder?.getAbsolutePath(LocalContext.current) + if (game is GBAgameData) "/${game.gbaGame.GameNum}.png" else "/${(game as GBgameData).gbgame.Serial}.png"
            if (!File(filepath).exists())
                filepath =
                    coverfilefolder?.getAbsolutePath(LocalContext.current) + if (game is GBAgameData) "/${
                        game.gbaGame.GameNum?.trimStart('0')
                    }a.png" else "/${(game as GBgameData).gbgame.Serial}.png"

            GlideImage(
                imageModel = { filepath },
                failure = {
                    Image(
                        painter = if (game is GBAgameData) painterResource(id = R.drawable.gameboyadvance) else painterResource(
                            id = R.drawable.gameboy
                        ),
                        contentDescription = "gba",
                        modifier = Modifier.fillMaxSize(),
                        alignment = Alignment.TopCenter
                    )
                },
                requestOptions = {
                    RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                },
                imageOptions = ImageOptions(
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.FillWidth,
                )
            )
            Text(
                if (game is GBAgameData)
                    "GBA"
                else "GB",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(
                        if (game is GBAgameData) Color.Green else Color.Yellow,
                        shape = CircleShape
                    )
                    .combinedClickable(
                        onClick = onclick
                    )
                    .padding(horizontal = 10.dp),

                )
        }

        Spacer(modifier = Modifier.width(3.dp))
        Column {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                fontWeight = FontWeight.Bold,
                text = if (game is GBAgameData) game.gbaGame.ChiGamename
                    ?: game.gbaDocumentFile.name ?: ""
                else (game as GBgameData).gbgame.EngGamename ?: game.gbDocumentFile.baseName ?: "",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                text = if (game is GBAgameData) game.gbaDocumentFile.name ?: ""
                else (game as GBgameData).gbDocumentFile.name ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Mgba_AndroidTheme {
    }
}