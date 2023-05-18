package hh.game.mgba_android.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.FileIOUtils
import hh.game.mgba_android.R
import hh.game.mgba_android.adapter.CheatListAdapter
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.utils.Cheat
import hh.game.mgba_android.utils.GBACheat
import hh.game.mgba_android.utils.GBAcheatUtils
import java.io.File

class CheatsActivity : AppCompatActivity() {
    private lateinit var cheatListAdapter: CheatListAdapter
    private var cheatListview : RecyclerView ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cheats)
        initView()
    }

    fun initView() {
//        binding.backbtn.setOnClickListener {
//            onBackPressed()
//        }
        cheatListview = findViewById(R.id.cheatListview)
        var gametype = intent.getStringExtra("gametype")
        var game = when (gametype) {
            "GBA" ->
                intent.getParcelableExtra<GBAgame>("gamedetail")

            else ->
                intent.getParcelableExtra<GBgame>("gamedetail")
        }
        cheatListAdapter = CheatListAdapter(this, ArrayList())
        if (gametype.equals("GBA")) {
            var gameNum = (game as GBAgame).GameNum
            if (GBAcheatUtils.generateInternalCheat(this, gameNum)) {
                var cheatList = ArrayList<Cheat>()
                var cheat = Cheat()
                File(getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cht").readLines()
                    .forEachIndexed { index, s ->
                        when {
                            s.contains("!disabled") -> {
                                if(!cheat.cheatTitle.equals("")&&!cheat.cheatCode.equals("")) {
                                    cheatList.add(cheat)
                                    cheat = Cheat()
                                }
                                cheat.isSelect = false
                            }
                            s.contains("!enabled") -> {
                                if(!cheat.cheatTitle.equals("")&&!cheat.cheatCode.equals("")) {
                                    cheatList.add(cheat)
                                    cheat = Cheat()
                                }
                                cheat.isSelect = true
                            }
                            s.contains("#") -> {
                                cheat.cheatTitle = s
                            }
                            else -> cheat.cheatCode += s + "\n"
                        }
                    }
                cheatListview?.layoutManager = LinearLayoutManager(this)
                cheatListAdapter.updateCheatList(cheatList)
                cheatListview?.adapter = cheatListAdapter
                cheatListAdapter.cheatOnCheckListener = { position, isSelect ->
                    cheatList.get(position).isSelect = isSelect
                    FileIOUtils.writeFileFromString(
                        getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cht",
                        GBACheat(cheatlist = cheatList).toString()
                    )
                    FileIOUtils.writeFileFromString(
                        getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cheats",
                        GBACheat(cheatlist = cheatList).toString()
                            .replace("!enabled\n","")
                    )
                }

            }
        }
    }
}