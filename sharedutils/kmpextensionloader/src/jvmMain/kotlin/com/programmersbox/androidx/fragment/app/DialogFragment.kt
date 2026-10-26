package androidx.fragment.app

import android.content.DialogInterface

/**
 * Supertype of CloudStream plugins' settings/donation dialogs. Desktop never creates or shows one, so the
 * lifecycle methods those dialogs override (onStart, onCreateDialog, ...) are left out.
 */
open class DialogFragment : Fragment(), DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {
    override fun onCancel(dialog: DialogInterface) {}

    override fun onDismiss(dialog: DialogInterface) {}
}
