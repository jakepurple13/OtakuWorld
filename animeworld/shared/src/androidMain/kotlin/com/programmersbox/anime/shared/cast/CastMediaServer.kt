package com.programmersbox.anime.shared.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.programmersbox.kmpmodels.KmpSubtitle
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class RegisteredSubtitle(val id: Int, val url: String, val cues: List<VttCue>)

/**
 * IPv4 of a Wi-Fi or Ethernet network that is not a VPN; null otherwise (the Chromecast cannot reach us).
 * Looks past the active network, which is the VPN when one is up.
 */
@Suppress("DEPRECATION") // allNetworks: the callback-based replacement can't answer synchronously.
fun wifiIpv4(context: Context): String? {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
    return cm.allNetworks.firstNotNullOfOrNull { network ->
        val caps = cm.getNetworkCapabilities(network) ?: return@firstNotNullOfOrNull null
        val local = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        if (!local || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@firstNotNullOfOrNull null
        cm.getLinkProperties(network)?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }
}

/**
 * Phone-side HTTP server the Chromecast pulls from: converted subtitles (`/sub`), header-requiring
 * streams proxied through us (`/p`), and downloaded files (`/f`).
 */
class CastMediaServer(
    private val okHttp: OkHttpClient,
    private val hostProvider: () -> String?,
    private val port: Int = 0,
    /** Sent upstream by the proxy unless the stream's own headers set a User-Agent. */
    private val userAgent: String? = null,
) {
    private class UpstreamException(cause: Throwable) : Exception(cause)

    private class ProxyTarget(val url: String, val headers: Map<String, String>)

    private val subtitles = ConcurrentHashMap<Int, String>()
    private val proxies = ConcurrentHashMap<String, ProxyTarget>()
    private val proxyTokens = ConcurrentHashMap<Pair<String, Map<String, String>>, String>()
    private val files = ConcurrentHashMap<String, File>()
    private val nextId = AtomicInteger()

    @Volatile
    private var server: Server? = null

    @Volatile
    var baseUrl: String? = null
        private set

    private val subtitleClient by lazy { okHttp.newBuilder().callTimeout(SUBTITLE_TIMEOUT_S, TimeUnit.SECONDS).build() }

    /** [CastResult.Started] once serving; [CastResult.NoWifi] without a Wi-Fi host; [CastResult.Failed] when the port won't bind. */
    fun start(): CastResult {
        if (server != null) return CastResult.Started
        val host = hostProvider() ?: return CastResult.NoWifi
        val s = Server()
        try {
            s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
        } catch (e: IOException) {
            Log.w(TAG, "Could not start cast server on port $port", e)
            return CastResult.Failed("Couldn't start local server")
        }
        server = s
        baseUrl = "http://$host:${s.listeningPort}"
        return CastResult.Started
    }

    fun stop() {
        server?.stop()
        server = null
        baseUrl = null
        subtitles.clear()
        proxies.clear()
        proxyTokens.clear()
        files.clear()
    }

    suspend fun registerSubtitle(sub: KmpSubtitle): RegisteredSubtitle? {
        val base = baseUrl ?: return null
        return try {
            val text = withContext(Dispatchers.IO) {
                if (sub.url.startsWith("file:")) {
                    // Downloaded episodes keep their subtitles next to the video, referenced as file: URIs.
                    File(URI(sub.url)).readText()
                } else {
                    subtitleClient.newCall(Request.Builder().url(sub.url).apply { sub.headers.forEach { (k, v) -> header(k, v) } }.build())
                        .execute()
                        .use { if (it.isSuccessful) it.body.string() else null }
                }
            } ?: return null
            val vtt = toWebVtt(text, SubtitleFormat.fromUrl(sub.url))
            val id = nextId.incrementAndGet()
            subtitles[id] = vtt
            RegisteredSubtitle(id, "$base/sub/$id.vtt", parseVttCues(vtt))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    fun registerStream(url: String, headers: Map<String, String>): String =
        "${baseUrl.orEmpty()}/p/${newProxyToken(url, headers)}"

    fun registerFile(file: File): String {
        val token = UUID.randomUUID().toString()
        files[token] = file
        return "${baseUrl.orEmpty()}/f/$token"
    }

    private fun newProxyToken(url: String, headers: Map<String, String>): String =
        proxyTokens.getOrPut(url to headers) {
            UUID.randomUUID().toString().also { proxies[it] = ProxyTarget(url, headers) }
        }

    private inner class Server : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            if (session.method == Method.OPTIONS) {
                return newFixedLengthResponse(Response.Status.NO_CONTENT, null, "").also {
                    it.addHeader("Access-Control-Allow-Origin", "*")
                    it.addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                    it.addHeader("Access-Control-Allow-Headers", "Range, Content-Type")
                }
            }
            val path = session.uri
            val range = session.headers["range"]
            val response = try {
                when {
                    path.startsWith("/sub/") ->
                        subtitles[path.removePrefix("/sub/").removeSuffix(".vtt").toIntOrNull()]
                            ?.let { newFixedLengthResponse(Response.Status.OK, "text/vtt; charset=utf-8", it) }
                    path.startsWith("/p/") -> proxies[path.removePrefix("/p/")]?.let { proxy(it, range) }
                    path.startsWith("/f/") -> files[path.removePrefix("/f/")]?.let { serveFile(it, range) }
                    else -> null
                }
            } catch (e: UpstreamException) {
                Log.w(TAG, "Upstream request failed for $path", e)
                newFixedLengthResponse(upstreamStatus(502), "text/plain", "bad gateway")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to serve $path", e)
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error")
            } ?: newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
            return response.also { it.addHeader("Access-Control-Allow-Origin", "*") }
        }

        private fun proxy(target: ProxyTarget, range: String?): Response {
            val request = Request.Builder().url(target.url).apply {
                target.headers.forEach { (k, v) -> header(k, v) }
                if (userAgent != null && target.headers.keys.none { it.equals("user-agent", ignoreCase = true) }) {
                    header("User-Agent", userAgent)
                }
                range?.let { header("Range", it) }
            }.build()
            val upstream = try {
                okHttp.newCall(request).execute()
            } catch (e: IOException) {
                throw UpstreamException(e)
            }
            try {
                return proxyResponse(target, upstream)
            } catch (e: Exception) {
                upstream.close()
                throw e
            }
        }

        private fun proxyResponse(target: ProxyTarget, upstream: okhttp3.Response): Response {
            val type = upstream.header("Content-Type").orEmpty()
            val isHls = type.substringBefore(';').trim().lowercase() in hlsTypes || upstream.request.url.encodedPath.endsWith(".m3u8", ignoreCase = true)
            if (isHls && upstream.isSuccessful) {
                val body = upstream.use { it.body.string() }
                val rewritten = rewriteHlsPlaylist(body, upstream.request.url.toString()) { child ->
                    "${baseUrl.orEmpty()}/p/${newProxyToken(child, target.headers)}"
                }
                return newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", rewritten)
            }
            val status = upstreamStatus(upstream.code)
            val body = upstream.body
            val length = body.contentLength()
            val mime = type.ifEmpty { "application/octet-stream" }
            val response =
                if (length >= 0) newFixedLengthResponse(status, mime, body.byteStream(), length)
                else newChunkedResponse(status, mime, body.byteStream())
            response.addHeader("Accept-Ranges", "bytes")
            upstream.header("Content-Range")?.let { response.addHeader("Content-Range", it) }
            return response
        }

        private fun serveFile(file: File, range: String?): Response {
            if (!file.isFile) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
            val total = file.length()
            val mime = when (file.extension.lowercase()) {
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                else -> "video/mp4"
            }
            val bounds = range?.let { parseRange(it, total) }
            if (range != null && bounds == null) {
                return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, "text/plain", "").also {
                    it.addHeader("Content-Range", "bytes */$total")
                }
            }
            val (start, end) = bounds ?: (0L to total - 1)
            val stream = FileInputStream(file).also { it.channel.position(start) }
            val length = end - start + 1
            val response = newFixedLengthResponse(
                if (bounds == null) Response.Status.OK else Response.Status.PARTIAL_CONTENT,
                mime,
                stream,
                length,
            )
            response.addHeader("Accept-Ranges", "bytes")
            if (bounds != null) response.addHeader("Content-Range", "bytes $start-$end/$total")
            return response
        }
    }

    private companion object {
        const val TAG = "CastMediaServer"
        const val SUBTITLE_TIMEOUT_S = 5L
        val hlsTypes = setOf("application/vnd.apple.mpegurl", "application/x-mpegurl")

        fun upstreamStatus(code: Int): NanoHTTPD.Response.IStatus = object : NanoHTTPD.Response.IStatus {
            override fun getRequestStatus() = code
            override fun getDescription() = "$code"
        }

        /** Parses `bytes=a-b`, `bytes=a-` and `bytes=-n` into an inclusive range, or null when unsatisfiable. */
        fun parseRange(header: String, total: Long): Pair<Long, Long>? {
            val spec = header.removePrefix("bytes=").substringBefore(',').trim()
            val (from, to) = spec.split('-', limit = 2).takeIf { it.size == 2 } ?: return null
            val start: Long
            val end: Long
            if (from.isEmpty()) {
                val suffix = to.toLongOrNull() ?: return null
                start = (total - suffix).coerceAtLeast(0)
                end = total - 1
            } else {
                start = from.toLongOrNull() ?: return null
                end = if (to.isEmpty()) total - 1 else (to.toLongOrNull() ?: return null).coerceAtMost(total - 1)
            }
            return if (start in 0..end) start to end else null
        }
    }
}
