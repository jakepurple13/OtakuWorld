package com.programmersbox.anime.shared.downloads

class UnsupportedHlsException(message: String) : Exception(message)

sealed interface HlsPlaylist {
    data class Master(
        val variants: List<HlsVariant>,
        val audioGroupsWithUri: Set<String> = emptySet(),
    ) : HlsPlaylist

    data class Media(
        val segments: List<HlsSegment>,
        val map: HlsMap?,
        val mediaSequence: Long,
        val live: Boolean = false,
    ) : HlsPlaylist
}

data class HlsVariant(
    val uri: String,
    val bandwidth: Long,
    val width: Int?,
    val height: Int?,
    val audioGroup: String? = null,
)

data class HlsSegment(val uri: String, val key: HlsKey?, val sequence: Long)

data class HlsKey(val method: String, val uri: String?, val iv: ByteArray?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HlsKey) return false
        return method == other.method && uri == other.uri && iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + (uri?.hashCode() ?: 0)
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        return result
    }
}

data class HlsMap(val uri: String)

fun parseHlsPlaylist(text: String, baseUrl: String): HlsPlaylist {
    val lines = text.removePrefix("\uFEFF").lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    if (lines.firstOrNull() != "#EXTM3U") throw UnsupportedHlsException("Not an HLS playlist")
    return if (lines.any { it.startsWith("#EXT-X-STREAM-INF") }) parseMaster(lines, baseUrl)
    else parseMedia(lines, baseUrl)
}

private fun parseMaster(lines: List<String>, baseUrl: String): HlsPlaylist.Master {
    val variants = mutableListOf<HlsVariant>()
    val audioGroupsWithUri = mutableSetOf<String>()
    var pending: Map<String, String>? = null
    for (line in lines) {
        when {
            line.startsWith("#EXT-X-STREAM-INF:") -> pending = parseAttributes(line.substringAfter(':'))
            line.startsWith("#EXT-X-MEDIA:") -> {
                val attrs = parseAttributes(line.substringAfter(':'))
                val group = attrs["GROUP-ID"]
                if (attrs["TYPE"] == "AUDIO" && attrs["URI"] != null && group != null) audioGroupsWithUri += group
            }

            line.startsWith("#") -> Unit
            pending != null -> {
                val attrs = pending
                val res = attrs["RESOLUTION"]?.split('x', 'X')
                variants += HlsVariant(
                    uri = resolveUrl(baseUrl, line),
                    bandwidth = attrs["BANDWIDTH"]?.toLongOrNull() ?: 0L,
                    width = res?.getOrNull(0)?.toIntOrNull(),
                    height = res?.getOrNull(1)?.toIntOrNull(),
                    audioGroup = attrs["AUDIO"],
                )
                pending = null
            }
        }
    }
    return HlsPlaylist.Master(variants, audioGroupsWithUri)
}

private fun parseMedia(lines: List<String>, baseUrl: String): HlsPlaylist.Media {
    val segments = mutableListOf<HlsSegment>()
    var map: HlsMap? = null
    var key: HlsKey? = null
    var mediaSequence = 0L
    var nextSequence = 0L
    var sequenceSet = false
    var ended = false
    for (line in lines) {
        when {
            line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> {
                mediaSequence = line.substringAfter(':').trim().toLongOrNull() ?: 0L
                nextSequence = mediaSequence
                sequenceSet = true
            }

            line.startsWith("#EXT-X-BYTERANGE") -> throw UnsupportedHlsException("EXT-X-BYTERANGE not supported")
            line.startsWith("#EXT-X-I-FRAMES-ONLY") -> throw UnsupportedHlsException("EXT-X-I-FRAMES-ONLY not supported")
            line.startsWith("#EXT-X-ENDLIST") -> ended = true

            line.startsWith("#EXT-X-KEY:") -> {
                val attrs = parseAttributes(line.substringAfter(':'))
                val keyFormat = attrs["KEYFORMAT"]
                if (keyFormat != null && keyFormat != "identity") continue
                val method = attrs["METHOD"] ?: "NONE"
                key = if (method == "NONE") null
                else HlsKey(
                    method = method,
                    uri = attrs["URI"]?.let { resolveUrl(baseUrl, it) },
                    iv = attrs["IV"]?.let(::parseHexIv),
                )
            }

            line.startsWith("#EXT-X-MAP:") -> {
                val attrs = parseAttributes(line.substringAfter(':'))
                if (attrs["BYTERANGE"] != null) throw UnsupportedHlsException("EXT-X-MAP BYTERANGE not supported")
                val uri = attrs["URI"]
                if (uri != null) {
                    val newMap = HlsMap(resolveUrl(baseUrl, uri))
                    if (map != null && map != newMap) throw UnsupportedHlsException("Multiple EXT-X-MAP init segments not supported")
                    map = newMap
                }
            }

            line.startsWith("#") -> Unit
            else -> {
                segments += HlsSegment(resolveUrl(baseUrl, line), key, nextSequence)
                nextSequence++
            }
        }
    }
    if (segments.isEmpty()) throw UnsupportedHlsException("Playlist has no segments")
    return HlsPlaylist.Media(segments, map, if (sequenceSet) mediaSequence else 0L, live = !ended)
}

/** Parses `KEY=VALUE,KEY="quoted, value"` attribute lists. Quotes are stripped from values. */
internal fun parseAttributes(list: String): Map<String, String> {
    val result = LinkedHashMap<String, String>()
    var i = 0
    while (i < list.length) {
        val eq = list.indexOf('=', i)
        if (eq < 0) break
        val name = list.substring(i, eq).trim().trimStart(',').trim()
        var j = eq + 1
        val value: String
        if (j < list.length && list[j] == '"') {
            val end = list.indexOf('"', j + 1).let { if (it < 0) list.length else it }
            value = list.substring(j + 1, end)
            j = end + 1
        } else {
            val end = list.indexOf(',', j).let { if (it < 0) list.length else it }
            value = list.substring(j, end).trim()
            j = end
        }
        result[name] = value
        i = j + 1
    }
    return result
}

private fun parseHexIv(value: String): ByteArray {
    val hex = value.removePrefix("0x").removePrefix("0X")
    if (hex.isEmpty() || hex.length > 32 || hex.any { it.digitToIntOrNull(16) == null }) {
        throw UnsupportedHlsException("Malformed EXT-X-KEY IV: $value")
    }
    val padded = hex.padStart(32, '0')
    return ByteArray(16) { padded.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

/** Minimal relative URL resolver (no java.net.URI in commonMain). */
internal fun resolveUrl(base: String, ref: String): String {
    if (Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(ref)) return ref
    val cleanBase = base.substringBefore('#').substringBefore('?')
    val schemeEnd = cleanBase.indexOf("://")
    if (ref.startsWith("//")) {
        return (if (schemeEnd >= 0) cleanBase.substring(0, schemeEnd + 1) else "https:") + ref
    }
    val hostEnd = if (schemeEnd >= 0) cleanBase.indexOf('/', schemeEnd + 3).let { if (it < 0) cleanBase.length else it }
    else cleanBase.indexOf('/').let { if (it < 0) cleanBase.length else it }
    return if (ref.startsWith("/")) {
        cleanBase.substring(0, hostEnd) + ref
    } else {
        val dirEnd = cleanBase.lastIndexOf('/')
        val dir = if (dirEnd >= hostEnd) cleanBase.substring(0, dirEnd + 1) else cleanBase.substring(0, hostEnd) + "/"
        dir + ref
    }
}

private val heightP = Regex("""(\d{3,4})\s*[pP]""")
private val resolutionX = Regex("""\d{3,4}\s*[xX]\s*(\d{3,4})""")

fun HlsPlaylist.Master.chooseVariant(qualityHint: String?): HlsVariant {
    if (qualityHint != null) {
        val heights = (resolutionX.findAll(qualityHint).map { it.groupValues[1] } +
                heightP.findAll(qualityHint).map { it.groupValues[1] })
            .mapNotNull { it.toIntOrNull() }
            .toList()
        for (h in heights) {
            variants.filter { it.height == h }.maxByOrNull { it.bandwidth }?.let { return it }
        }
    }
    return variants.maxByOrNull { it.bandwidth } ?: error("Master playlist has no variants")
}

fun aesIv(segment: HlsSegment): ByteArray {
    segment.key?.iv?.let { return it }
    val iv = ByteArray(16)
    var seq = segment.sequence
    for (i in 15 downTo 8) {
        iv[i] = (seq and 0xFF).toByte()
        seq = seq shr 8
    }
    return iv
}
