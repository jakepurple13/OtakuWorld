// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: desktop version of the androidMain shim, same companion API. Never instantiated (the
// constructor follows the desktop Application stub); the host sets `context` to its desktop
// Application. Storage is DataStore over that Application's file-backed SharedPreferences.
package com.lagradost.cloudstream3

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import com.lagradost.api.setContext
import com.lagradost.cloudstream3.utils.AppContextUtils.openBrowser
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getKeys
import com.lagradost.cloudstream3.utils.DataStore.removeKey
import com.lagradost.cloudstream3.utils.DataStore.removeKeys
import com.lagradost.cloudstream3.utils.DataStore.setKey
import java.io.File
import java.lang.ref.WeakReference

class CloudStreamApp(packageName: String, baseDataDir: File) : Application(packageName, baseDataDir) {

    companion object {
        /** Use to get Activity from Context. */
        // OTAKU: desktop contexts never wrap an Activity, so there is no ContextWrapper unwrapping.
        fun Context.getActivity(): Activity? = this as? Activity

        private var _context: WeakReference<Context>? = null
        var context
            get() = _context?.get()
            internal set(value) {
                _context = WeakReference(value)
                setContext(value)
            }

        fun <T : Any> getKeyClass(path: String, valueType: Class<T>): T? {
            return context?.getKey(path, valueType)
        }

        fun <T : Any> setKeyClass(path: String, value: T) {
            context?.setKey(path, value)
        }

        fun removeKeys(folder: String): Int? {
            return context?.removeKeys(folder)
        }

        fun <T> setKey(path: String, value: T) {
            context?.setKey(path, value)
        }

        fun <T> setKey(folder: String, path: String, value: T) {
            context?.setKey(folder, path, value)
        }

        inline fun <reified T : Any> getKey(path: String, defVal: T?): T? {
            return context?.getKey(path, defVal)
        }

        inline fun <reified T : Any> getKey(path: String): T? {
            return context?.getKey(path)
        }

        inline fun <reified T : Any> getKey(folder: String, path: String): T? {
            return context?.getKey(folder, path)
        }

        inline fun <reified T : Any> getKey(folder: String, path: String, defVal: T?): T? {
            return context?.getKey(folder, path, defVal)
        }

        fun getKeys(folder: String): List<String>? {
            return context?.getKeys(folder)
        }

        fun removeKey(folder: String, path: String) {
            context?.removeKey(folder, path)
        }

        fun removeKey(path: String) {
            context?.removeKey(path)
        }

        /** Desktop has no WebView fallback; [fallbackWebView] and [fragment] are ignored. */
        fun openBrowser(url: String, fallbackWebView: Boolean = false, fragment: Fragment? = null) {
            context?.openBrowser(url, fallbackWebView, fragment)
        }

        @Suppress("UNUSED_PARAMETER")
        fun openBrowser(url: String, activity: Activity?) {
            openBrowser(url, false, null)
        }
    }
}
