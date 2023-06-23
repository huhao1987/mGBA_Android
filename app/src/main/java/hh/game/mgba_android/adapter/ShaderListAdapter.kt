package hh.game.mgba_android.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hh.game.mgba_android.R
import hh.game.mgba_android.utils.Cheat

class ShaderListAdapter(var context: Context, var list: ArrayList<Cheat>) :
    RecyclerView.Adapter<ShaderListAdapter.ViewHolder>() {
    private var cheatlist = list
    var cheatOnCheckListener: CheatOnCheckListener? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val view = view
        val cheatName = view.findViewById<TextView>(R.id.cheatName)
        val chooseCheck = view.findViewById<CheckBox>(R.id.chooseCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.cheat_row, null))

    override fun getItemCount(): Int = cheatlist.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var cheat = cheatlist[position]
        if (cheat.isSelect) holder.chooseCheck.isChecked = true
        else holder.chooseCheck.isChecked = false
        holder.cheatName.text = cheat.cheatTitle.replace("# ","")
        holder.chooseCheck.setOnClickListener {
            if (cheat.isSelect) cheat.isSelect = false
            else cheat.isSelect = true
            cheatOnCheckListener?.invoke(position,cheat.isSelect)
        }
    }

    fun updateCheatList(list: ArrayList<Cheat>){
        cheatlist = list
        notifyDataSetChanged()
    }
}