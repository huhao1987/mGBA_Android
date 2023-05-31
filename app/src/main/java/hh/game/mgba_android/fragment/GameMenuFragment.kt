package hh.game.mgba_android.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import hh.game.mgba_android.R

class GameMenuFragment : DialogFragment() {
    private var onMenuListener : OnMenuListener?= null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game_menu,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.exitebtn).setOnClickListener {
            onMenuListener?.onExit()
        }
    }
    fun setOndismissListener(listener: OnMenuListener){
        onMenuListener = listener
    }
    override fun onDismiss(dialog: DialogInterface) {
        onMenuListener?.onDismiss()
        super.onDismiss(dialog)
    }
}

interface OnMenuListener{
    fun onDismiss()
    fun onSaveState()
    fun onLoadState()
    fun onExit()
}