package androidx.fragment.app

import androidx.activity.ComponentActivity

/**
 * CloudStream plugins cast `CommonActivity.activity` to this (or AppCompatActivity) to show settings
 * dialogs. Desktop never creates one, so that activity is always null there.
 */
abstract class FragmentActivity : ComponentActivity() {
    open fun getSupportFragmentManager(): FragmentManager =
        throw UnsupportedOperationException("No fragments on desktop")
}
