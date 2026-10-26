package com.programmersbox.anime.shared.downloads

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.ceil

/**
 * One downloadable DASH representation. [initUrl] is null when the media needs no separate
 * initialization segment (a single-file representation is one entry in [segmentUrls]).
 */
data class DashRepresentation(
    val id: String,
    val mimeType: String,
    val bandwidth: Long,
    val height: Int?,
    val initUrl: String?,
    val segmentUrls: List<String>,
)

/** The representations of a static MPD's period, split into video-only, audio-only and muxed (video with audio). */
data class DashManifest(
    val video: List<DashRepresentation>,
    val audio: List<DashRepresentation>,
    val muxed: List<DashRepresentation>,
)

/**
 * Parses a static MPD into absolute segment URLs, resolving `BaseURL`s at every level against
 * [manifestUrl]. Throws [UnsupportedOperationException] for live (`type="dynamic"`), DRM-protected
 * and multi-period manifests.
 */
fun parseDashManifest(xml: String, manifestUrl: String): DashManifest {
    val mpd = parseXml(xml)
    if (mpd.localName != "MPD") throw IOException("Not a DASH manifest")
    if (mpd.attr("type") == "dynamic") throw UnsupportedOperationException("Live DASH")
    if (mpd.getElementsByTagNameNS("*", "ContentProtection").length > 0) {
        throw UnsupportedOperationException("DRM-protected DASH streams are not supported")
    }
    val periods = mpd.children("Period")
    if (periods.isEmpty()) throw IOException("DASH manifest has no Period")
    val period = periods.singleOrNull() ?: throw UnsupportedOperationException("Multi-period DASH is not supported")
    val periodSeconds = period.attr("duration")?.let(::parseIsoDuration)
        ?: mpd.attr("mediaPresentationDuration")?.let(::parseIsoDuration)
            ?.minus(period.attr("start")?.let(::parseIsoDuration) ?: 0.0)

    val periodBase = manifestUrl.toHttpUrl().withBaseUrl(mpd).withBaseUrl(period)
    val video = mutableListOf<DashRepresentation>()
    val audio = mutableListOf<DashRepresentation>()
    val muxed = mutableListOf<DashRepresentation>()
    for (set in period.children("AdaptationSet")) {
        val setBase = periodBase.withBaseUrl(set)
        for (rep in set.children("Representation")) {
            val mime = rep.attr("mimeType") ?: set.attr("mimeType") ?: ""
            val target = when (kindOf(set, rep, mime)) {
                Kind.Video -> video
                Kind.Audio -> audio
                Kind.Muxed -> muxed
                null -> continue
            }
            val id = rep.attr("id") ?: throw IOException("DASH Representation without an id")
            val bandwidth = rep.attr("bandwidth")?.toLongOrNull() ?: 0L
            val (initUrl, segmentUrls) = segments(
                levels = listOf(period, set, rep),
                base = setBase.withBaseUrl(rep),
                hasOwnBaseUrl = rep.child("BaseURL") != null || set.child("BaseURL") != null,
                id = id,
                bandwidth = bandwidth,
                periodSeconds = periodSeconds,
            )
            target += DashRepresentation(
                id = id,
                mimeType = mime,
                bandwidth = bandwidth,
                height = (rep.attr("height") ?: set.attr("height"))?.toIntOrNull(),
                initUrl = initUrl,
                segmentUrls = segmentUrls,
            )
        }
    }
    return DashManifest(video, audio, muxed)
}

/**
 * The video to download and the audio to merge with it: the highest-bandwidth video no taller than
 * [maxHeight] (else the shortest one), and the highest-bandwidth audio. Muxed representations are
 * used only when there is no separate video, and need no audio.
 */
fun DashManifest.choose(maxHeight: Int = 1080): Pair<DashRepresentation, DashRepresentation?> {
    val candidates = video.ifEmpty { muxed }
    if (candidates.isEmpty()) throw IOException("DASH manifest has no video")
    val picked = candidates.filter { (it.height ?: 0) <= maxHeight }.maxByOrNull { it.bandwidth }
        ?: candidates.minBy { it.height ?: 0 }
    return picked to if (video.isEmpty()) null else audio.maxByOrNull { it.bandwidth }
}

private enum class Kind { Video, Audio, Muxed }

private val audioCodecs = listOf("mp4a", "opus", "vorbis", "ac-3", "ec-3", "flac", "mp3")

/** Null for anything that is neither video nor audio (subtitles, thumbnails). */
private fun kindOf(set: Element, rep: Element, mime: String): Kind? {
    val contentType = set.attr("contentType") ?: rep.attr("contentType")
    val codecs = (rep.attr("codecs") ?: set.attr("codecs")).orEmpty().lowercase().split(',').map { it.trim() }
    val components = set.children("ContentComponent").mapNotNull { it.attr("contentType") }.toSet()
    return when {
        mime.startsWith("video/") || contentType == "video" ->
            if ("audio" in components || codecs.any { codec -> audioCodecs.any { codec.startsWith(it) } }) Kind.Muxed else Kind.Video
        mime.startsWith("audio/") || contentType == "audio" -> Kind.Audio
        else -> null
    }
}

/**
 * The init URL and media URLs of a representation, from the most specific of `SegmentTemplate`,
 * `SegmentList` or `SegmentBase` along [levels] (Period, AdaptationSet, Representation).
 * Without segment information, or with only byte ranges, the representation's `BaseURL` is the whole file.
 */
private fun segments(
    levels: List<Element>,
    base: HttpUrl,
    hasOwnBaseUrl: Boolean,
    id: String,
    bandwidth: Long,
    periodSeconds: Double?,
): Pair<String?, List<String>> {
    val nearest = levels.reversed().firstNotNullOfOrNull { level ->
        level.child("SegmentTemplate") ?: level.child("SegmentList") ?: level.child("SegmentBase")
    }
    fun resolve(relative: String) = base.resolve(relative)?.toString() ?: throw IOException("Bad DASH URL: $relative")
    fun singleFile(): Pair<String?, List<String>> {
        if (!hasOwnBaseUrl) throw IOException("DASH Representation $id has no segments")
        return null to listOf(base.toString())
    }

    return when (nearest?.localName) {
        "SegmentTemplate" -> {
            val templates = levels.mapNotNull { it.child("SegmentTemplate") }
            fun attr(name: String) = templates.asReversed().firstNotNullOfOrNull { it.attr(name) }
            val media = attr("media") ?: throw IOException("DASH SegmentTemplate without media")
            val init = attr("initialization")
                ?: templates.asReversed().firstNotNullOfOrNull { it.child("Initialization")?.attr("sourceURL") }
            val timescale = attr("timescale")?.toLongOrNull() ?: 1L
            val startNumber = attr("startNumber")?.toLongOrNull() ?: 1L
            val timeline = templates.asReversed().firstNotNullOfOrNull { it.child("SegmentTimeline") }
            val times: List<Long?> = if (timeline != null) {
                val offset = attr("presentationTimeOffset")?.toLongOrNull() ?: 0L
                val end = periodSeconds?.let { offset + (it * timescale).toLong() }
                expandTimeline(timeline, end)
            } else {
                val duration = attr("duration")?.toLongOrNull()?.takeIf { it > 0 }
                    ?: throw IOException("DASH SegmentTemplate without duration or SegmentTimeline")
                val seconds = periodSeconds ?: throw IOException("DASH manifest has no duration")
                List(ceil(seconds * timescale / duration - 1e-9).toInt()) { null }
            }
            val initUrl = init?.let { resolve(expandTemplate(it, id, bandwidth, number = null, time = null)) }
            initUrl to times.mapIndexed { i, time -> resolve(expandTemplate(media, id, bandwidth, startNumber + i, time)) }
        }

        "SegmentList" -> {
            val urls = nearest.children("SegmentURL").map { it.attr("media") }
            if (urls.isEmpty() || urls.any { it == null }) singleFile()
            else nearest.child("Initialization")?.attr("sourceURL")?.let(::resolve) to urls.map { resolve(it!!) }
        }

        else -> singleFile()
    }
}

/** Start times of a `SegmentTimeline`; a negative `r` repeats until the next `S@t` or [end]. */
private fun expandTimeline(timeline: Element, end: Long?): List<Long> {
    val entries = timeline.children("S")
    val times = mutableListOf<Long>()
    var t = 0L
    entries.forEachIndexed { i, s ->
        t = s.attr("t")?.toLongOrNull() ?: t
        val d = s.attr("d")?.toLongOrNull()?.takeIf { it > 0 } ?: throw IOException("DASH S element without d")
        var r = s.attr("r")?.toLongOrNull() ?: 0L
        if (r < 0) {
            val limit = entries.getOrNull(i + 1)?.attr("t")?.toLongOrNull() ?: end
                ?: throw IOException("DASH SegmentTimeline repeats to an unknown end")
            r = ceil((limit - t).toDouble() / d).toLong() - 1
        }
        repeat((r + 1).toInt()) {
            times += t
            t += d
        }
    }
    return times
}

private val templateIdentifier = Regex("""\$(RepresentationID|Number|Bandwidth|Time)?(?:%0(\d+)d)?\$""")

/** Substitutes `$RepresentationID$`, `$Number$`, `$Bandwidth$`, `$Time$` (optionally `%0<width>d`) and `$$`. */
internal fun expandTemplate(template: String, id: String, bandwidth: Long, number: Long?, time: Long?): String =
    templateIdentifier.replace(template) { match ->
        val value: Any? = when (match.groupValues[1]) {
            "" -> "$"
            "RepresentationID" -> id
            "Number" -> number
            "Bandwidth" -> bandwidth
            else -> time
        }
        val text = value?.toString() ?: throw IOException("DASH template ${match.value} has no value in $template")
        match.groupValues[2].toIntOrNull()?.let { text.padStart(it, '0') } ?: text
    }

private val isoDuration = Regex("""P(?:(\d+(?:\.\d+)?)D)?(?:T(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?)?""")

/** Seconds in an `xs:duration` with day, hour, minute and second parts (e.g. `PT1H2M3.5S`); null otherwise. */
internal fun parseIsoDuration(value: String): Double? {
    val match = isoDuration.matchEntire(value.trim()) ?: return null
    val (days, hours, minutes, seconds) = match.destructured
    return listOf(days to 86_400.0, hours to 3_600.0, minutes to 60.0, seconds to 1.0)
        .sumOf { (part, unit) -> (part.toDoubleOrNull() ?: 0.0) * unit }
}

private fun parseXml(xml: String): Element {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        // Manifests come from third-party sources: no DOCTYPEs, so no external entities.
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        isExpandEntityReferences = false
    }
    return factory.newDocumentBuilder().parse(InputSource(StringReader(xml))).documentElement
}

/** [this] resolved against [element]'s first `BaseURL` child, if it has one. */
private fun HttpUrl.withBaseUrl(element: Element): HttpUrl {
    val text = element.child("BaseURL")?.textContent?.trim()?.takeIf { it.isNotEmpty() } ?: return this
    return resolve(text) ?: throw IOException("Bad DASH BaseURL: $text")
}

private fun Element.children(name: String): List<Element> {
    val nodes = childNodes
    return (0 until nodes.length).mapNotNull { i -> (nodes.item(i) as? Element)?.takeIf { it.localName == name } }
}

private fun Element.child(name: String): Element? = children(name).firstOrNull()

private fun Element.attr(name: String): String? = if (hasAttribute(name)) getAttribute(name) else null
