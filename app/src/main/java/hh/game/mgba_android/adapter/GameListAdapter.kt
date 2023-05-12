package hh.game.mgba_android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.anggrayudi.storage.file.getAbsolutePath
import hh.game.mgba_android.R
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.utils.GameDetailsListener
import hh.game.mgba_android.utils.Gametype
import hh.game.mgba_android.utils.Gameutils
import kotlinx.coroutines.runBlocking

class GameListAdapter(var context: Context, var list : ArrayList<DocumentFile>) : RecyclerView.Adapter<GameListAdapter.ViewHolder>() {
    private var gamelist = list
    var itemClickListener : ItemClickListener?=null
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gamename = view.findViewById<TextView>(R.id.gamename)
        val gametype = view.findViewById<TextView>(R.id.gametype)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.game_row, null))


    override fun getItemCount(): Int = gamelist.size

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        runBlocking {
            var game = gamelist[position]
            var gbaUtil = Gameutils(context,game.getAbsolutePath(context)).init()
            var gameType = when{
                game.getAbsolutePath(context).contains(".gba",true) -> Gametype.GBA
                game.getAbsolutePath(context).contains(".gb",true) -> Gametype.GB
                else -> null
            }
            gbaUtil.loadGames(gameType,object : GameDetailsListener {
                override fun onGetDetails(gameDetails: Any?) {
                    if(gameDetails != null) {
                        var gameTitle =
                            when (gameDetails) {
                                is GBAgame -> {
                                    holder.gametype.background = context.getDrawable(R.drawable.icon_bg_gba)
                                    holder.gametype.text = "GBA"
                                    (gameDetails as GBAgame).ChiGamename
                                }
                                else -> {
                                    holder.gametype.background = context.getDrawable(R.drawable.icon_bg_gb)
                                    holder.gametype.text = "GB"
                                    (gameDetails as GBgame).EngGamename
                                }
                            }
                        holder.gamename.text = (gameTitle?:game.name).toString()
                    }
                    else
                        holder.gamename.text = game.name

                    holder.gamename.setOnClickListener {
                        itemClickListener?.invoke(position,gameDetails)
                    }
                }
            })

        }
    }

    private suspend fun getGameTitle(game:DocumentFile,view: TextView){
        

    }
    fun updateList(list : ArrayList<DocumentFile>){
        this.gamelist = list
        notifyDataSetChanged()
    }
}

typealias ItemClickListener = (Int,Any?) -> Unit