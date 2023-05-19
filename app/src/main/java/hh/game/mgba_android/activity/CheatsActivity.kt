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
import com.blankj.utilcode.util.FileIOUtils
import com.google.android.material.card.MaterialCardView
import hh.game.mgba_android.R
import hh.game.mgba_android.adapter.CheatListAdapter
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.databinding.ActivityCheatsBinding
import hh.game.mgba_android.utils.Cheat
import hh.game.mgba_android.utils.GBACheat
import hh.game.mgba_android.utils.GBAcheatUtils
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
        findViewById<Button>(R.id.switchbtn).setOnClickListener {
            if (cheatListview!!.isVisible) {
                editorLayout?.visibility = View.VISIBLE
                cheatListview?.visibility = View.GONE
                cheateditor?.setText(File(getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cht").readText())
                saveBtn?.setOnClickListener {
                    GBAcheatUtils.saveCheatToFile(this, gameNum!!, cheateditor?.text.toString())
                    cheatListAdapter.updateCheatList(getCheatList())
                }
            } else {
                editorLayout?.visibility = View.GONE
                cheatListview?.visibility = View.VISIBLE
            }
        }
        var gametype = intent.getStringExtra("gametype")
        var game = when (gametype) {
            "GBA" ->
                intent.getParcelableExtra<GBAgame>("gamedetail").also {
                    findViewById<TextView>(R.id.gametitle).text = it?.ChiGamename
                }

            else ->
                intent.getParcelableExtra<GBgame>("gamedetail").also {
                    findViewById<TextView>(R.id.gametitle).text = it?.EngGamename
                }
        }
        cheatListAdapter = CheatListAdapter(this, ArrayList())
        if (gametype.equals("GBA")) {
            gameNum = (game as GBAgame).GameNum
            if (GBAcheatUtils.generateInternalCheat(this, gameNum)) {
                var cheatList = getCheatList()
                cheatListview?.layoutManager = LinearLayoutManager(this)
                cheatListAdapter.updateCheatList(cheatList)
                cheatListview?.adapter = cheatListAdapter
                cheatListAdapter.cheatOnCheckListener = { position, isSelect ->
                    cheatList.get(position).isSelect = isSelect
                    GBAcheatUtils.saveCheatToFile(
                        this,
                        gameNum!!,
                        GBACheat(cheatlist = cheatList).toString()
                    )
                }

            }
        }
    }

    fun getCheatList(): ArrayList<Cheat> {
        var cheatList = ArrayList<Cheat>()
        var cheat = Cheat()
        File(getExternalFilesDir("cheats")?.absolutePath + "/$gameNum.cht").readLines()
            .forEachIndexed { index, s ->
                when {
                    s.contains("!disabled") -> {
                        if (!cheat.cheatTitle.equals("") && !cheat.cheatCode.equals("")) {
                            cheatList.add(cheat)
                            cheat = Cheat()
                        }
                        cheat.isSelect = false
                    }

                    s.contains("!enabled") -> {
                        if (!cheat.cheatTitle.equals("") && !cheat.cheatCode.equals("")) {
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
        return cheatList
    }
}