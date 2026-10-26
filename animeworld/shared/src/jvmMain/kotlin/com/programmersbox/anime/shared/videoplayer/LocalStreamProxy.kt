package com.programmersbox.anime.shared.videoplayer

import com.programmersbox.anime.shared.cast.rewriteHlsPlaylist
import fi.iki.elonen.NanoHTTPD
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Loopback HTTP proxy that lets libVLC play streams which need request headers.
 *
 * URL shape: `http://127.0.0.1:{port}/{token}/{base64url(upstream directory)}/{file}?{query}`. The path keeps
 * the upstream file name and directory, so relative URIs inside HLS playlists and DASH manifests resolve back
 * through the proxy on their own; manifests are still rewritten so absolute URIs and redirects work too.
 * [register] mints one unguessable token per stream; every hop of that stream reuses the token's headers.
 */
class LocalStreamProxy(private val client: OkHttpClient = OkHttpClient()) : AutoCloseable {
    private class UpstreamException(cause: Throwable) : Exception(cause)

    private class Stream(val url: String, val headers: Map<String, String>, val mimeType: String?)

    private val streams = ConcurrentHashMap<String, Stream>()
    private val random = SecureRandom()

    @Volatile
    private var server: Server? = null

    @Synchronized
    fun start() {
        if (server != null) return
        server = Server().also { it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true) }
    }

    /** URL libVLC should open for [upstreamUrl], fetched upstream with [headers] on every hop. */
    fun register(upstreamUrl: String, headers: Map<String, String>, mimeType: String?): String {
        val s = checkNotNull(server) { "LocalStreamProxy not started" }
        val token = ByteArray(18).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        streams[token] = Stream(upstreamUrl, headers, mimeType)
        return proxied(s.listeningPort, token, upstreamUrl.toHttpUrl())
    }

    /** Stops the server and invalidates all tokens. */
    @Synchronized
    override fun close() {
        server?.stop()
        server = null
        streams.clear()
    }

    private fun proxied(port: Int, token: String, upstream: HttpUrl): String {
        val clean = upstream.newBuilder().query(null).fragment(null).build()
        val path = clean.encodedPath
        val dir = clean.toString().substringBeforeLast('/') + "/"
        val rest = path.substring(path.lastIndexOf('/') + 1)
        val query = upstream.encodedQuery?.let { "?$it" }.orEmpty()
        val encodedDir = Base64.getUrlEncoder().withoutPadding().encodeToString(dir.toByteArray())
        return "http://$HOST:$port/$token/$encodedDir/$rest$query"
    }

    private inner class Server : NanoHTTPD(HOST, 0) {
        override fun serve(session: IHTTPSession): Response {
            val parts = session.uri.removePrefix("/").split('/', limit = 3)
            val stream = parts.getOrNull(0)?.let { streams[it] }
            val dir = parts.getOrNull(1)?.let { runCatching { String(Base64.getUrlDecoder().decode(it)) }.getOrNull() }
            val upstream = if (stream == null || dir == null || parts.size < 3) null else upstreamUrl(dir, parts[2], session)
            if (stream == null || upstream == null) return text(Response.Status.NOT_FOUND, "not found")
            return try {
                forward(parts[0], stream, upstream, session.headers["range"])
            } catch (_: UpstreamException) {
                text(upstreamStatus(502), "bad gateway")
            } catch (_: Exception) {
                text(Response.Status.INTERNAL_ERROR, "error")
            }
        }

        private fun upstreamUrl(dir: String, rest: String, session: IHTTPSession): HttpUrl? =
            dir.toHttpUrlOrNull()?.newBuilder()?.addPathSegments(rest)?.encodedQuery(session.queryParameterString)?.build()

        private fun forward(token: String, stream: Stream, url: HttpUrl, range: String?): Response {
            val hintedType = stream.mimeType.takeIf { url.toString() == stream.url }
            val path = url.encodedPath
            val isManifest = hintedType.isHls() || hintedType.isDash() ||
                path.endsWith(".m3u8", true) || path.endsWith(".mpd", true)
            val request = Request.Builder().url(url).apply {
                stream.headers.forEach { (k, v) -> header(k, v) }
                // Manifests are rewritten, so a byte range would no longer match the body.
                if (range != null && !isManifest) header("Range", range)
            }.build()
            val upstream = try {
                client.newCall(request).execute()
            } catch (e: IOException) {
                throw UpstreamException(e)
            }
            try {
                return respond(token, upstream, hintedType)
            } catch (e: Exception) {
                upstream.close()
                throw e
            }
        }

        private fun respond(token: String, upstream: okhttp3.Response, hintedType: String?): Response {
            val type = upstream.header("Content-Type").orEmpty()
            val finalUrl = upstream.request.url
            val path = finalUrl.encodedPath
            val isHls = hintedType.isHls() || type.isHls() || path.endsWith(".m3u8", true)
            val isDash = hintedType.isDash() || type.isDash() || path.endsWith(".mpd", true)
            if ((isHls || isDash) && upstream.isSuccessful) {
                val body = upstream.use { it.body.string() }
                return if (isHls) {
                    val out = rewriteHlsPlaylist(body, finalUrl.toString()) { proxied(listeningPort, token, it.toHttpUrl()) }
                    newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", out)
                } else {
                    newFixedLengthResponse(Response.Status.OK, "application/dash+xml", rewriteMpd(body, finalUrl, token))
                }
            }
            val body = upstream.body
            val length = body.contentLength()
            val mime = type.ifEmpty { "application/octet-stream" }
            val status = upstreamStatus(upstream.code)
            val response =
                if (length >= 0) newFixedLengthResponse(status, mime, body.byteStream(), length)
                else newChunkedResponse(status, mime, body.byteStream())
            response.addHeader("Accept-Ranges", "bytes")
            upstream.header("Content-Range")?.let { response.addHeader("Content-Range", it) }
            return response
        }

        /** Rewrites `BaseURL`s and absolute attribute URLs; injects a `BaseURL` (the final MPD dir) when there is none. */
        private fun rewriteMpd(body: String, base: HttpUrl, token: String): String {
            var out = baseUrlTag.replace(body) { m ->
                val abs = base.resolve(m.groupValues[1].trim())
                "<BaseURL>" + (abs?.let { proxied(listeningPort, token, it) } ?: m.groupValues[1]) + "</BaseURL>"
            }
            if (!baseUrlTag.containsMatchIn(body)) {
                val dir = proxied(listeningPort, token, base).substringBefore('?').substringBeforeLast('/') + "/"
                mpdOpen.find(out)?.let { m -> out = out.replaceRange(m.range, m.value + "<BaseURL>$dir</BaseURL>") }
            }
            return absoluteAttr.replace(out) { m ->
                val abs = m.groupValues[2].toHttpUrlOrNull()
                m.groupValues[1] + "=\"" + (abs?.let { proxied(listeningPort, token, it) } ?: m.groupValues[2]) + "\""
            }
        }
    }

    private companion object {
        const val HOST = "127.0.0.1"
        val baseUrlTag = Regex("<BaseURL>([^<]*)</BaseURL>")
        val mpdOpen = Regex("<MPD[^>]*>")
        val absoluteAttr = Regex("(media|initialization|sourceURL)=\"(https?://[^\"]+)\"")
        val hlsTypes = setOf("application/vnd.apple.mpegurl", "application/x-mpegurl")

        fun String?.isHls() = this != null && substringBefore(';').trim().lowercase() in hlsTypes
        fun String?.isDash() = this != null && substringBefore(';').trim().equals("application/dash+xml", true)

        fun text(status: NanoHTTPD.Response.IStatus, body: String) =
            NanoHTTPD.newFixedLengthResponse(status, "text/plain", body)

        fun upstreamStatus(code: Int): NanoHTTPD.Response.IStatus = object : NanoHTTPD.Response.IStatus {
            override fun getRequestStatus() = code
            override fun getDescription() = "$code"
        }
    }
}
