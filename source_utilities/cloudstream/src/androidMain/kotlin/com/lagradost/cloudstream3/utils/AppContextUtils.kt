// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: subset. Only helpers plugins call that do not depend on CloudStream's UI, settings or
// TV integration are kept.
package com.lagradost.cloudstream3.utils

import android.app.Activity.RESULT_CANCELED
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.text.Spanned
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpanned
import androidx.fragment.app.Fragment
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.CommonActivity.activity
import com.lagradost.cloudstream3.mvvm.logError

object AppContextUtils {
    fun String?.html(): Spanned {
        return getHtmlText(this ?: return "".toSpanned())
    }

    private fun getHtmlText(text: String): Spanned {
        return try {
            // I have no idea if this can throw any error, but I dont want to try
            HtmlCompat.fromHtml(
                text, HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } catch (e: Exception) {
            logError(e)
            text.toSpanned()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun openWebView(fragment: Fragment?, url: String) {
        // OTAKU: no-op; AnimeWorld has no CloudStream WebView screen to navigate to.
    }

    /**
     * If fallbackWebview is true and a fragment is supplied then it will open a webview with the url if the browser fails.
     * */
    fun Context.openBrowser(
        url: String,
        fallbackWebview: Boolean = false,
        fragment: Fragment? = null,
    ) = (this.getActivity() ?: activity)?.runOnUiThread {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = url.toUri()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            // activityResultRegistry is used to fall back to webview if a browser is missing
            // On older versions the startActivity just crashes, but on newer android versions
            // You need to check the result to make sure it failed
            val activityResultRegistry = fragment?.activity?.activityResultRegistry
            if (activityResultRegistry != null) {
                activityResultRegistry.register(
                    url,
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_CANCELED && fallbackWebview) {
                        openWebView(fragment, url)
                    }
                }.launch(intent)
            } else this.startActivity(intent)
        } catch (e: Exception) {
            logError(e)
            if (fallbackWebview) {
                openWebView(fragment, url)
            }
        }
    }

    fun Context.isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    // Deprecate after next stable
    /* @Deprecated(
        message = "Use splitUrlParameters instead.",
        replaceWith = ReplaceWith(
            expression = "splitUrlParameters(url.toString())",
            imports = ["com.lagradost.cloudstream3.splitUrlParameters"],
        ),
        level = DeprecationLevel.WARNING,
    ) */
    fun splitQuery(url: java.net.URL): Map<String, String> {
        return com.lagradost.cloudstream3.splitUrlParameters(url.toString())
    }

    fun Context.isUsingMobileData(): Boolean {
        val connectionManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork: Network? = connectionManager.activeNetwork
            val networkCapabilities = connectionManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true &&
                    !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            connectionManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
        }
    }

    fun Context.isAppInstalled(uri: String): Boolean {
        // OTAKU: upstream goes through Wrappers.packageManager (a test seam).
        val pm = packageManager

        return try {
            pm.getPackageInfo(uri, 0) // PackageManager.GET_ACTIVITIES
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
