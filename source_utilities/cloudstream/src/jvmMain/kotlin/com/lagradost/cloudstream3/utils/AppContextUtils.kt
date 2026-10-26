// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: desktop version of the androidMain subset, same members and JVM signatures. HTML becomes
// plain text, the browser is the system browser, and the network checks report a wired connection.
package com.lagradost.cloudstream3.utils

import android.content.Context
import android.text.Html
import android.text.Spanned
import androidx.fragment.app.Fragment
import com.lagradost.cloudstream3.mvvm.logError
import java.awt.Desktop
import java.net.URI

object AppContextUtils {
    fun String?.html(): Spanned {
        return Html.fromHtml(this.orEmpty(), Html.FROM_HTML_MODE_LEGACY)
    }

    @Suppress("UNUSED_PARAMETER")
    fun openWebView(fragment: Fragment?, url: String) {
        // OTAKU: no-op; there is no WebView on desktop.
    }

    /** Opens [url] in the system browser when the desktop supports it; otherwise does nothing. */
    // Returns Unit? like the Android version (whose body is `activity?.runOnUiThread {}`), keeping the JVM signature.
    @Suppress("UNUSED_PARAMETER")
    fun Context.openBrowser(
        url: String,
        fallbackWebview: Boolean = false,
        fragment: Fragment? = null,
    ): Unit? {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            logError(e)
        }
        return Unit
    }

    @Suppress("UnusedReceiverParameter")
    fun Context.isNetworkAvailable(): Boolean = true

    fun splitQuery(url: java.net.URL): Map<String, String> {
        return com.lagradost.cloudstream3.splitUrlParameters(url.toString())
    }

    @Suppress("UnusedReceiverParameter")
    fun Context.isUsingMobileData(): Boolean = false

    // Plugins ask about Android packages, which are never installed on desktop.
    @Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
    fun Context.isAppInstalled(uri: String): Boolean = false
}
