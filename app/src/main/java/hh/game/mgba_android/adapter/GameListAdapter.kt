package hh.game.mgba_android.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.anggrayudi.storage.file.getAbsolutePath
import com.bumptech.glide.Glide
import hh.game.mgba_android.R
import hh.game.mgba_android.database.GB.GBgame
import hh.game.mgba_android.database.GB.GBgameData
import hh.game.mgba_android.database.GBA.GBAgame
import hh.game.mgba_android.database.GBA.GBAgameData
import hh.game.mgba_android.utils.GameDetailsListener
import hh.game.mgba_android.utils.Gametype
import hh.game.mgba_android.utils.Gameutils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class GameListAdapter(var context: Context, var list: ArrayList<Any>) :
    RecyclerView.Adapter<GameListAdapter.ViewHolder>() {
    private var gamelist = list
    var itemClickListener: ItemClickListener? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gamename = view.findViewById<TextView>(R.id.gamename)
        val gametype = view.findViewById<TextView>(R.id.gametype)
        val gamecover = view.findViewById<ImageView>(R.id.gamecover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.game_row, null))

    override fun getItemCount(): Int = gamelist.size

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        runBlocking {
            var game = gamelist[position]
            when (game) {
                is GBAgameData -> {
                    holder.gametype.background =
                        context.getDrawable(R.drawable.icon_bg_gba)
                    holder.gametype.text = "GBA"
                    holder.gamename.text = game.gbaGame.ChiGamename
                }

                is GBgameData -> {
                    holder.gametype.background =
                        context.getDrawable(R.drawable.icon_bg_gb)
                    holder.gametype.text = "GB"
                    holder.gamename.text = game.gBgame.EngGamename
                }
            }
            holder.gamename.setOnClickListener {
                itemClickListener?.invoke(position, game)
            }
        }
    }

    fun updateList(list: ArrayList<Any>) {
        this.gamelist = list
        notifyDataSetChanged()
    }
}

typealias ItemClickListener = (Int, Any?) -> Unit