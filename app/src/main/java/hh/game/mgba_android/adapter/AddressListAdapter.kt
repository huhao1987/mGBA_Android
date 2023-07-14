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

class AddressListAdapter(var list: ArrayList<Pair<Int,Int>>) :
    RecyclerView.Adapter<AddressListAdapter.ViewHolder>() {
    private var addresslist = list
    var addressOnClickListener: AddressOnClickListener? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val view = view
        val address = view.findViewById<TextView>(R.id.address)
        val value = view.findViewById<TextView>(R.id.value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.address_row, null))

    override fun getItemCount(): Int = addresslist.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var address = addresslist[position]
        holder.address.text =  address.first.toString(16).padStart(8, '0').toUpperCase()
        holder.value.text =  address.second.toString(16).padStart(8, '0').toUpperCase()
        holder.view.setOnClickListener {
            addressOnClickListener?.invoke(position,address)
        }
    }

    fun updateList(list: ArrayList<Pair<Int,Int>>){
        addresslist = list
        notifyDataSetChanged()
    }
}
typealias AddressOnClickListener = (Int,Pair<Int,Int>) -> Unit