package hh.game.mgba_android

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class GameMenuFragment : DialogFragment() {
    private var onMenuListener : OnMenuListener ?= null
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

        }
    }
    fun setOndismissListener(listener:OnMenuListener){
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