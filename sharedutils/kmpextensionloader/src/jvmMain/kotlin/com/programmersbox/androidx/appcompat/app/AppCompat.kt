package androidx.appcompat.app

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity

/**
 * CloudStream plugins check `CommonActivity.activity is AppCompatActivity` before showing settings or
 * donation dialogs. Desktop never creates one, so the check is false there. The SDK's AppCompatCallback,
 * TaskStackBuilder.SupportParentable and ActionBarDrawerToggle.DelegateProvider interfaces are left out:
 * no extension references them.
 */
abstract class AppCompatActivity : FragmentActivity()

/** SDK supertype of material's BottomSheetDialogFragment. Desktop never creates one. */
open class AppCompatDialogFragment : DialogFragment()
