package hh.game.mgba_android.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import hh.game.mgba_android.R

class PopDialogFragment(var title : String): DialogFragment() {
    private var onDialogClickListener : OnDialogClickListener ?= null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.apply {
            setTitle(title)
            setPositiveButton(
                R.string.ok,
                DialogInterface.OnClickListener { dialog, id ->
                    onDialogClickListener?.onPostive()
                    dialog.dismiss()
                })
            setNegativeButton(
                R.string.cancel,
                DialogInterface.OnClickListener { dialog, id ->
                    onDialogClickListener?.onNegative()
                    dialog.dismiss()
                })
        }
        return builder.create()
    }
    override fun onDismiss(dialog: DialogInterface) {
        onDialogClickListener?.onDismiss()
        super.onDismiss(dialog)
    }
    fun setOnDialogClickListener(listener: OnDialogClickListener){
        onDialogClickListener = listener
    }
}
interface OnDialogClickListener{
    fun onPostive()
    fun onNegative()
    fun onDismiss()
}