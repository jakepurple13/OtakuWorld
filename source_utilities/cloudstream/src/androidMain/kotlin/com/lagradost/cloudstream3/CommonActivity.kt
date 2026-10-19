// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: shim. Keeps `activity` and the showToast overloads that do not need UiText; the theme,
// PiP, focus and key handling of CloudStream's activity are dropped.
package com.lagradost.cloudstream3

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import com.lagradost.cloudstream3.mvvm.logError
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

    private var currentToast: Toast? = null

    fun showToast(@StringRes message: Int, duration: Int? = null) {
        val act = activity ?: return
        act.runOnUiThread {
            showToast(act, act.getString(message), duration)
        }
    }

    fun showToast(message: String?, duration: Int? = null) {
        val act = activity ?: return
        act.runOnUiThread {
            showToast(act, message, duration)
        }
    }

    /** duration is Toast.LENGTH_SHORT if null*/
    @MainThread
    fun showToast(act: Activity?, @StringRes message: Int, duration: Int? = null) {
        if (act == null) return
        showToast(act, act.getString(message), duration)
    }

    const val TAG = "COMPACT"

    /** duration is Toast.LENGTH_SHORT if null*/
    @MainThread
    fun showToast(act: Activity?, message: String?, duration: Int? = null) {
        if (act == null || message == null) {
            Log.w(TAG, "invalid showToast act = $act message = $message")
            return
        }
        Log.i(TAG, "showToast = $message")

        try {
            currentToast?.cancel()
        } catch (e: Exception) {
            logError(e)
        }

        try {
            // OTAKU: plain toast instead of CloudStream's custom toast layout.
            val toast = Toast.makeText(act, message.trim(), duration ?: Toast.LENGTH_SHORT)
            currentToast = toast
            toast.show()
        } catch (e: Exception) {
            logError(e)
        }
    }
}
