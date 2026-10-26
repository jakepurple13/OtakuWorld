// Copied from aniyomiorg/aniyomi 97414446b8a95994c72dd33c41c971a89d4d25b8, Apache-2.0. Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.animesource.model

import android.util.Log
import fi.iki.elonen.NanoHTTPD

// OTAKU: bind loopback only (upstream NanoHTTPD(0) binds every interface, exposing the server to the LAN).
open class HttpServer : NanoHTTPD("127.0.0.1", 0) {
    val url: String
        get() = "http://localhost:$listeningPort"

    fun isRunning(): Boolean {
        return isRunning
    }

    @Volatile
    private var isRunning = false

    override fun start() {
        try {
            super.start()
            isRunning = true
        } catch (e: Exception) {
            // OTAKU: android.util.Log instead of logcat (LogcatExtensions is Android-only; the JVM has a Log stub).
            Log.d("HttpServer", "Failed to start http server", e)
        }
    }

    override fun stop() {
        super.stop()
        isRunning = false
    }

    companion object {
        const val PLACEHOLDER_URL = "http://localhost:1"
    }
}
