package com.programmersbox.kmpuiviews.domain.exceptions

import com.programmersbox.kmpuiviews.domain.updatecheck.offlineRegex

/** A stored exception (`Throwable.stackTraceToString()` text) split into the parts the Exceptions screen shows. */
data class ParsedException(
    /** Fully qualified class from line 1, e.g. `java.net.SocketTimeoutException`. */
    val type: String,
    /** Rest of line 1 plus continuation lines before the first frame. */
    val message: String,
    val frames: List<Frame>,
    /** `Caused by: …` lines, trimmed. */
    val causes: List<String>,
    val extension: ExtensionRef?,
    val kind: ExceptionKind,
    val headline: ExceptionHeadline,
    val httpCode: Int?,
) {
    val shortType: String get() = type.substringAfterLast('.')
    val firstLine: String get() = message.lineSequence().first()

    /** Identifies "the same error": counts repeats and shares one AI explanation. */
    val groupKey: String get() = "$type|$firstLine|${extension?.id}"
}

/** One `at …` line. Library frames are folded by default. */
data class Frame(val text: String, val isLibrary: Boolean)

/** The Mihon/Aniyomi extension a trace passed through. */
data class ExtensionRef(val packageName: String, val lang: String, val name: String) {
    val id: String get() = "$lang.$name"
}

enum class ExceptionKind { Network, Blocked, Parsing, Source, App }

enum class ExceptionHeadline { TimedOut, Offline, Blocked, RateLimited, NotFound, ServerError, Network, Parsing, Source, App }

private val libraryPrefixes = listOf(
    "java.", "javax.", "kotlin.", "kotlinx.", "android.", "androidx.", "okhttp3.", "okio.",
    "org.jsoup.", "dalvik.", "sun.", "jdk.", "com.android.",
)
private val extensionFrame = Regex("""eu\.kanade\.tachiyomi\.(extension|animeextension)\.([a-z]+)\.([a-z0-9]+)\.""")
private val httpCodeRegex = Regex("""Status=(\d{3})|HTTP error (\d{3})|HTTP (\d{3})""")
private val timeoutRegex = Regex("""SocketTimeoutException|InterruptedIOException.*timeout""")
private val networkRegex = Regex("""SSLException|ConnectException|^java\.net\.""")
private val parsingRegex = Regex("""SerializationException|JsonDecodingException|JsonSyntaxException|MalformedJsonException|Selector""")

fun parseException(text: String): ParsedException {
    val lines = text.lines()
    val first = lines.firstOrNull().orEmpty()
    val prefix = first.substringBefore(": ", missingDelimiterValue = first)
    val hasTypePrefix = first.contains(": ") && !prefix.contains(' ')
    val type = if (hasTypePrefix) prefix else first
    val rest = lines.drop(1)
    val continuation = rest.takeWhile { !it.isTraceLine() }
    val message = (listOfNotNull(if (hasTypePrefix) first.substringAfter(": ") else null) + continuation)
        .joinToString("\n")
        .let { if (!hasTypePrefix && continuation.isEmpty()) "" else it }

    val trimmed = rest.map { it.trim() }
    val frames = trimmed.filter { frameLine.matches(it) }.map { line ->
        Frame(line, libraryPrefixes.any { line.removePrefix("at ").startsWith(it) })
    }
    val causes = trimmed.filter { it.startsWith("Caused by: ") }

    val extension = frames.firstNotNullOfOrNull { extensionFrame.find(it.text) }?.let { m ->
        val (kind, lang, name) = m.destructured
        ExtensionRef("eu.kanade.tachiyomi.$kind.$lang.$name", lang, name)
    }

    // Classify on line 1 and every cause, so a wrapped timeout still reads as a timeout.
    val checked = listOf(first) + causes.map { it.removePrefix("Caused by: ") }
    val httpCode = checked.firstNotNullOfOrNull { line ->
        httpCodeRegex.find(line)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.toInt()
    }
    val headline = when {
        httpCode == 429 -> ExceptionHeadline.RateLimited
        httpCode == 403 || httpCode == 503 -> ExceptionHeadline.Blocked
        httpCode == 404 || httpCode == 410 -> ExceptionHeadline.NotFound
        httpCode != null && httpCode in 500..599 -> ExceptionHeadline.ServerError
        checked.any { timeoutRegex.containsMatchIn(it) } -> ExceptionHeadline.TimedOut
        checked.any { offlineRegex.containsMatchIn(it) } -> ExceptionHeadline.Offline
        httpCode != null || checked.any { networkRegex.containsMatchIn(it) } -> ExceptionHeadline.Network
        checked.any { parsingRegex.containsMatchIn(it) } -> ExceptionHeadline.Parsing
        extension != null -> ExceptionHeadline.Source
        else -> ExceptionHeadline.App
    }

    return ParsedException(
        type = type,
        message = message,
        frames = frames,
        causes = causes,
        extension = extension,
        kind = headline.kind,
        headline = headline,
        httpCode = httpCode,
    )
}

private val ExceptionHeadline.kind: ExceptionKind
    get() = when (this) {
        ExceptionHeadline.RateLimited, ExceptionHeadline.Blocked -> ExceptionKind.Blocked
        ExceptionHeadline.NotFound, ExceptionHeadline.ServerError, ExceptionHeadline.TimedOut,
        ExceptionHeadline.Offline, ExceptionHeadline.Network -> ExceptionKind.Network
        ExceptionHeadline.Parsing -> ExceptionKind.Parsing
        ExceptionHeadline.Source -> ExceptionKind.Source
        ExceptionHeadline.App -> ExceptionKind.App
    }

// A real frame is "at pkg.Class.method(File.kt:12)"; message text that starts with "at " is not.
private val frameLine = Regex("""^at [^\s(]+\(.*\)$""")
private val moreLine = Regex("""^\.\.\. \d+ more$""")

private fun String.isTraceLine(): Boolean {
    val t = trim()
    return frameLine.matches(t) || t.startsWith("Caused by: ") || moreLine.matches(t) || t.startsWith("Suppressed: ")
}
