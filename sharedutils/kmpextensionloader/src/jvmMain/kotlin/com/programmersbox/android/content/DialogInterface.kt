package android.content

import android.view.KeyEvent

/**
 * CloudStream plugins' settings dialogs implement these listeners with synthetic lambda classes, which the
 * JVM can only load if the interfaces exist. Desktop never shows a dialog.
 */
interface DialogInterface {
    fun cancel()
    fun dismiss()

    fun interface OnCancelListener {
        fun onCancel(dialog: DialogInterface)
    }

    fun interface OnDismissListener {
        fun onDismiss(dialog: DialogInterface)
    }

    fun interface OnClickListener {
        fun onClick(dialog: DialogInterface, which: Int)
    }

    fun interface OnKeyListener {
        fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean
    }
}
