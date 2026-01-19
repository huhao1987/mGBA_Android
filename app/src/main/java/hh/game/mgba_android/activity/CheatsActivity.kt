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
                val parentDir = if (gamePath != null) File(gamePath).parentFile else null
                CheatUtils.saveCheatToFile(
                    this,
                    gameNum!!,
                    GBACheat(cheatlist = cheatList).toString(),
                    parentDir
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
                        
                        val parentDir = if (gamePath != null) File(gamePath).parentFile else null
                        var userFile = File(parentDir, "$gameNum.cheats")
                        
                        // Check if Game Dir file exists, if not, check Private Dir
                        var loadedFrom = "None"
                        if (userFile.exists()) {
                            loadedFrom = "Game Dir"
                        } else {
                            userFile = File(getExternalFilesDir("cheats"), "$gameNum.cheats")
                            if (userFile.exists()) loadedFrom = "Private Dir"
                        }
                        
                        if (loadedFrom != "None") {
                            cheateditor?.setText(userFile.readText())
                            android.widget.Toast.makeText(this@CheatsActivity, "Loaded from $loadedFrom: ${userFile.name}", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            cheateditor?.setText("")
                            android.widget.Toast.makeText(this@CheatsActivity, "No saved cheats found.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        
                        saveBtn?.setOnClickListener {
                            CheatUtils.saveCheatToFile(this@CheatsActivity, gameNum!!, cheateditor?.text.toString(), parentDir)
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
                    3 -> { // Pro (AR DS)
                        cheatListview?.visibility = View.GONE
                         editorLayout?.visibility = View.VISIBLE
                        assetsListView?.visibility = View.GONE

                        cheateditor?.hint = "Enter Action Replay DS codes:\nXXXXXXXX YYYYYYYY\nD9000000 02001234"
                        cheateditor?.setText("")
                        saveBtn?.text = "Apply to Engine"
                        
                        saveBtn?.setOnClickListener {
                            val rawText = cheateditor?.text.toString()
                            val lines = rawText.split("\n")
                            // (activity as GameActivity).resetARDSCheats() // We can't cast directly if not in GameActivity context? 
                            // Wait, CheatsActivity is separate. We need to send intent or binding?
                            // Actually, CheatsActivity runs ON TOP of GameActivity usually? No, it's a separate Activity.
                            // We need to pass data back or use a singleton/service.
                            // But usually intents are used.
                            
                            // Ah, mGBA Android architecture usually has GameActivity launch CheatsActivity.
                            // Communication back happens via onActivityResult or shared Prefs/Files.
                            // But these logic cheats are RUNTIME memory constructs. They might not persist easily in old formats?
                            // Or we save them to a file distinct from .cheats? e.g. .ards
                            
                            // For now, let's save to a file, and make GameActivity load it on Resume.
                             val parentDir = if (gamePath != null) File(gamePath).parentFile else null
                             val ardsFile = File(parentDir, "$gameNum.ards")
                             ardsFile.writeText(rawText)
                             android.widget.Toast.makeText(this@CheatsActivity, "Saved AR DS Cheats!", android.widget.Toast.LENGTH_SHORT).show()
                             // The GameActivity will need to load this file.
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
        val parentDir = if (gamePath != null) File(gamePath).parentFile else null
        
        // 1. Try to load user saved cheats from Game Directory
        if (parentDir != null) {
            val gameDirFile = File(parentDir, "$gameNum.cheats")
            if (gameDirFile.exists()) {
                 val userCheats = CheatUtils.parseUserCheatFile(gameDirFile)
                 if (userCheats.isNotEmpty()) {
                     Log.d("CheatsActivity", "Loaded from Game Dir: ${gameDirFile.absolutePath}")
                     return userCheats
                 }
            }
        }

        // 2. Try App Private Directory
        val userFile = File(getExternalFilesDir("cheats"), "$gameNum.cheats")
        if (userFile.exists()) {
             val userCheats = CheatUtils.parseUserCheatFile(userFile)
             if (userCheats.isNotEmpty()) {
                 Log.d("CheatsActivity", "Loaded from Private Dir: ${userFile.absolutePath}")
                 return userCheats
             }
        }
    
        // 3. Fallback to Database
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