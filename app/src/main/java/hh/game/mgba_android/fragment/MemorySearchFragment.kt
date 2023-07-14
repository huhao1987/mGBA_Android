package hh.game.mgba_android.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hh.game.mgba_android.R
import hh.game.mgba_android.adapter.AddressListAdapter

class MemorySearchFragment : DialogFragment() {
    private var onMemSearchListener: OnMemSearchListener? = null
    private lateinit var addressListAdapter: AddressListAdapter
    private var onAddressClickListener: OnAddressClickListener? = null
    private lateinit var searchResult: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_memory_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.exitBtn).setOnClickListener {
            onMemSearchListener?.onExit()
        }
        view.findViewById<Button>(R.id.searchBtn).setOnClickListener {
            onMemSearchListener?.onSearch(
                view.findViewById<EditText>(R.id.searchValue).text.toString().toInt()
            )
        }
        view.findViewById<Button>(R.id.newsearchBtn).setOnClickListener {
            onMemSearchListener?.onNewSearch(
                view.findViewById<EditText>(R.id.searchValue).text.toString().toInt()
            )
        }
        searchResult = view.findViewById<RecyclerView>(R.id.searchResult)
        addressListAdapter = AddressListAdapter(ArrayList())
        searchResult.layoutManager = LinearLayoutManager(context)
        searchResult.adapter = addressListAdapter
        addressListAdapter.addressOnClickListener = { position, address ->
            onAddressClickListener?.onClick(address)
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    fun setOnAddressClickListener(onAddressClickListener: OnAddressClickListener) {
        this.onAddressClickListener = onAddressClickListener
    }

    fun setOnMemSearchListener(listener: OnMemSearchListener) {
        onMemSearchListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    fun updateMemAddressList(list: ArrayList<Pair<Int, Int>>) {
        addressListAdapter.updateList(list)
    }
}

interface OnAddressClickListener {
    fun onClick(address: Pair<Int, Int>)
}

interface OnMemSearchListener {
    fun onSearch(value: Int)
    fun onNewSearch(value: Int)
    fun onExit()
}