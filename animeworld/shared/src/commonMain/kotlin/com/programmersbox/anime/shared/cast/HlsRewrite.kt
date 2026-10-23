package com.programmersbox.anime.shared.cast

import com.programmersbox.anime.shared.downloads.resolveUrl

private val uriAttribute = Regex("""URI="([^"]*)"""")

/**
 * Points every URI in an HLS playlist at [proxy]. URI lines and `URI="..."` attributes are resolved
 * against [baseUrl] first; all other lines are left as they were.
 */
fun rewriteHlsPlaylist(text: String, baseUrl: String, proxy: (absoluteUrl: String) -> String): String =
    text.split("\n").joinToString("\n") { line ->
        val body = line.trimEnd('\r')
        val tail = line.substring(body.length)
        when {
            body.isBlank() -> line
            body.startsWith("#") ->
                uriAttribute.replace(body) { m ->
                    "URI=\"${proxy(resolveUrl(baseUrl, m.groupValues[1]))}\""
                } + tail
            else -> proxy(resolveUrl(baseUrl, body.trim())) + tail
        }
    }
