package hh.game.mgba_android.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import hh.game.mgba_android.R
import hh.game.mgba_android.adapter.CheatListAdapter
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.utils.Cheat
import hh.game.mgba_android.utils.GBACheat
import hh.game.mgba_android.utils.CheatUtils
import hh.game.mgba_android.utils.CheatDatabaseUtils
import hh.game.mgba_android.utils.CheatDbEntry
import java.io.File

class CheatsActivity : AppCompatActivity() {
    private lateinit var cheatListAdapter: CheatListAdapter
    private var cheatListview: RecyclerView? = null
    private var editorLayout: MaterialCardView? = null
    private var cheateditor: EditText? = null
    private var gameNum: String? = ""
    private var saveBtn: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cheats)
        initView()
    }

    fun initView() {
        findViewById<ImageView>(R.id.backbtn).setOnClickListener {
            onBackPressed()
        }
        cheatListview = findViewById(R.id.cheatListview)
        editorLayout = findViewById(R.id.editorLayout)
        cheateditor = findViewById(R.id.cheateditor)
        saveBtn = findViewById(R.id.saveBtn)
        
        var gametype = intent.getStringExtra("gametype")
        gamePath = intent.getStringExtra("gamepath")
        
        var game = when (gametype) {
            "GBA" ->
                intent.getParcelableExtra<GBAgame>("gamedetail").also {
                    findViewById<TextView>(R.id.gametitle).text = it?.GameNum +"-"+it?.ChiGamename
                }

            else ->
                intent.getParcelableExtra<GBgame>("gamedetail").also {
                    findViewById<TextView>(R.id.gametitle).text = it?.EngGamename
                }
        }
        // gameBean no longer needed
        
        if (gametype.equals("GBA")) {
            val extraGameNum = intent.getStringExtra("cheat")
            if (!extraGameNum.isNullOrEmpty()) {
                gameNum = extraGameNum
            } else {
                gameNum = (game as GBAgame).GameNum
            }
            
            if (CheatUtils.generateCheat(this, gameNum)) {
                // Initial load handled below
            }
        }

        cheatListAdapter = CheatListAdapter(this, ArrayList())
        cheatListview?.layoutManager = LinearLayoutManager(this)
        cheatListview?.adapter = cheatListAdapter
        
        fun loadCurrentCheats() {
            var cheatList = getCheatList()
            cheatListAdapter.updateCheatList(cheatList)
            cheatListAdapter.cheatOnCheckListener = { position, isSelect ->
                cheatList.get(position).isSelect = isSelect
                CheatUtils.saveCheatToFile(
                    this,
                    gameNum!!,
                    GBACheat(cheatlist = cheatList).toString()
                )
            }
        }

        loadCurrentCheats()

        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.cheattablayout)
        val assetsListView = findViewById<RecyclerView>(R.id.assetsCheatListview)
        
        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Current
                        cheatListview?.visibility = View.VISIBLE
                        editorLayout?.visibility = View.GONE
                        assetsListView?.visibility = View.GONE
                        loadCurrentCheats()
                    }
                    1 -> { // Edit
                        cheatListview?.visibility = View.GONE
                        editorLayout?.visibility = View.VISIBLE
                        assetsListView?.visibility = View.GONE
                        cheateditor?.setText(File(getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats").let {
                            if(!it.exists()) it.createNewFile()
                            it
                        }.readText())
                        saveBtn?.setOnClickListener {
                            CheatUtils.saveCheatToFile(this@CheatsActivity, gameNum!!, cheateditor?.text.toString())
                            // No automatic switch back, user stays in edit
                        }
                    }
                    2 -> { // Assets / Database
                        Log.d("CheatsActivity", "Assets tab selected. GameNum: '$gameNum'")
                        cheatListview?.visibility = View.GONE
                        editorLayout?.visibility = View.GONE
                        assetsListView?.visibility = View.VISIBLE
                        
                        var cheatsToShow: ArrayList<Cheat>? = null
                        
                        // 1. Try Assets
                        try {
                            val assetName = "gbacheats/$gameNum.cht"
                            Log.d("CheatsActivity", "Trying to load asset: $assetName")
                            val assetStream = assets.open(assetName)
                            val gbaCheat = CheatUtils().convertECcodestoVba(assetStream, false)
                            cheatsToShow = gbaCheat.cheatlist
                            Log.d("CheatsActivity", "Loaded from legacy assets: ${cheatsToShow?.size}")
                        } catch (e: Exception) {
                            Log.d("CheatsActivity", "Legacy asset load failed or not found.")
                        }

                        // 2. Try Unified DB if assets failed
                        if (cheatsToShow.isNullOrEmpty()) {
                            Log.d("CheatsActivity", "Trying CheatDatabaseUtils lookup for: '$gameNum'")
                            val dbCheats = CheatDatabaseUtils.getCheatsForGame(this@CheatsActivity, gameNum ?: "")
                            if (dbCheats != null) {
                                cheatsToShow = CheatDatabaseUtils.convertToCheatList(dbCheats)
                                Log.d("CheatsActivity", "Loaded from DB: ${cheatsToShow?.size}")
                            } else {
                                Log.d("CheatsActivity", "No cheats found in DB.")
                            }
                        }

                        val adapter = CheatListAdapter(this@CheatsActivity, cheatsToShow ?: ArrayList())
                        assetsListView?.layoutManager = LinearLayoutManager(this@CheatsActivity)
                        assetsListView?.adapter = adapter
                        
                        adapter.cheatOnCheckListener = { position, isSelect ->
                             // Optional: Allow selecting to import?
                             // For now, read-only or copy logic could go here
                        }
                    }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private var gamePath: String? = null

    fun getCheatList(): ArrayList<Cheat> {
        var parentDir: File? = null
        if (gamePath != null) {
            parentDir = File(gamePath).parentFile
        }

        val dbCheats = CheatDatabaseUtils.getCheatsForGame(this, gameNum ?: "", parentDir)
        return if (!dbCheats.isNullOrEmpty()) {
            CheatDatabaseUtils.convertToCheatList(dbCheats)
        } else {
            ArrayList()
        }
    }

    override fun onBackPressed() {
        val resultCode = RESULT_OK
        setResult(resultCode)
        super.onBackPressed()
    }
}