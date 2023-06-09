package hh.game.mgba_android.adapter

import android.annotation.SuppressLint
import android.content.Context
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
import hh.game.mgba_android.database.GB.GBgameData
import hh.game.mgba_android.database.GBA.GBAgameData
import kotlinx.coroutines.runBlocking

class GameListAdapter(var context: Context, var list: ArrayList<Any>) :
    RecyclerView.Adapter<GameListAdapter.ViewHolder>() {
    private var gamelist = list
    var itemClickListener: ItemClickListener? = null
    var itemOnLongClickListener: ItemOnLongClickListener? = null
    private var coverfolder: DocumentFile? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val view = view
        val gamename = view.findViewById<TextView>(R.id.gamename)
        val gametype = view.findViewById<TextView>(R.id.gametype)
        val gamecover = view.findViewById<ImageView>(R.id.gamecover)
        val gamepath = view.findViewById<TextView>(R.id.gamepath)
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
                    holder.gamepath.text = game.gbaDocumentFile.getAbsolutePath(context)
//                    var image = coverfolder?.findFile("${game.gbaGame.GameNum}.png")?.getAbsolutePath(context)
                    var image =
                        coverfolder?.getAbsolutePath(context) + "/${game.gbaGame.GameNum}.png"

                    Glide.with(context)
                        .load(image)
                        .into(holder.gamecover)
                }

                is GBgameData -> {
                    holder.gametype.background =
                        context.getDrawable(R.drawable.icon_bg_gb)
                    holder.gametype.text = "GB"
                    holder.gamename.text = game.gbgame.EngGamename
                    holder.gamepath.text = game.gbDocumentFile.getAbsolutePath(context)
                }
            }
            holder.view.setOnClickListener {
                itemClickListener?.invoke(position, game)
            }
            holder.view.setOnLongClickListener {
                itemOnLongClickListener?.invoke(position,game)
                true
            }
        }
    }

    fun updateList(list: ArrayList<Any>) {
        this.gamelist = list
        notifyDataSetChanged()
    }

    fun updateCoverfolder(folder: DocumentFile?) {
        this.coverfolder = folder
        notifyDataSetChanged()
    }
}
typealias ItemOnLongClickListener = (Int, Any?) -> Unit
typealias ItemClickListener = (Int, Any?) -> Unit