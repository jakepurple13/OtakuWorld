// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: desktop version of the androidMain shim, same members. There is no Activity on desktop
// (`activity` stays null unless set), and toasts are logged to stdout.
package com.lagradost.cloudstream3

import android.app.Activity
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

object CommonActivity {

    private var _activity: WeakReference<Activity>? = null
    var activity
        get() = _activity?.get()
        private set(value) {
            _activity = WeakReference(value)
        }

    @MainThread
    fun setActivityInstance(newActivity: Activity?) {
        activity = newActivity
    }

    fun showToast(@StringRes message: Int, duration: Int? = null) {
        showToast(activity, message, duration)
    }

    fun showToast(message: String?, duration: Int? = null) {
        showToast(activity, message, duration)
    }

    /** duration is Toast.LENGTH_SHORT if null*/
    @Suppress("UNUSED_PARAMETER")
    @MainThread
    fun showToast(act: Activity?, @StringRes message: Int, duration: Int? = null) {
        // No plugin resources on desktop; log the id.
        println("$TAG showToast = string resource #$message")
    }

    const val TAG = "COMPACT"

    /** duration is Toast.LENGTH_SHORT if null*/
    @Suppress("UNUSED_PARAMETER")
    @MainThread
    fun showToast(act: Activity?, message: String?, duration: Int? = null) {
        if (message == null) return
        println("$TAG showToast = ${message.trim()}")
    }
}
