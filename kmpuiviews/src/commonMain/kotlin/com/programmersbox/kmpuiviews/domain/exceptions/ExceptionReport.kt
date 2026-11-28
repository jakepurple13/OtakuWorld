package com.programmersbox.kmpuiviews.domain.exceptions

import com.programmersbox.kmpuiviews.domain.updatecheck.KnownError
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Where and what the report says it came from. */
data class ReportContext(
    val appName: String,
    val appVersion: String,
    val device: String,
    val timeZone: TimeZone,
)

/** Markdown report for Copy, Share and Save…. The AI explanation is never included. */
fun buildExceptionReport(
    rawText: String,
    parsed: ParsedException,
    time: Long,
    sourceName: String?,
    extensionVersion: String?,
    seenThisWeek: Int,
    ctx: ReportContext,
): String = buildString {
    appendLine("### ${ctx.appName} error report")
    appendLine("- App: ${ctx.appName} ${ctx.appVersion}")
    appendLine("- Device: ${ctx.device}")
    appendLine("- When: ${localDateTime(time, ctx.timeZone).format()} (${ctx.timeZone.id})")
    if (sourceName != null) {
        // Don't repeat the id when it's all we know (extension not installed).
        val ext = parsed.extension
            ?.let { e -> listOfNotNull(e.id.takeIf { it != sourceName }, extensionVersion?.let { "v$it" }).joinToString(" ") }
            ?.takeIf { it.isNotEmpty() }
        appendLine("- Source: $sourceName" + (ext?.let { " ($it)" } ?: ""))
    }
    appendLine("- Seen: $seenThisWeek× in the last 7 days")
    appendLine()
    appendLine("```")
    appendLine(rawText)
    append("```")
}

/** e.g. "MangaWorld error: Comick JsonDecodingException". */
fun reportSubject(appName: String, sourceName: String?, parsed: ParsedException): String =
    "$appName error: ${sourceName ?: "App"} ${parsed.shortType}"

/** e.g. "MangaWorld-error-2026-11-27-AsuraScans.md". */
fun reportFileName(appName: String, time: Long, sourceName: String?, timeZone: TimeZone): String {
    val date = localDateTime(time, timeZone).date
    return "$appName-error-$date-${(sourceName ?: "App").filterNot { it.isWhitespace() }}.md"
}

/** The fixed explanation for this error, or null when it needs AI. */
fun ParsedException.knownError(): KnownError? = when (headline) {
    ExceptionHeadline.TimedOut -> KnownError.TimedOut
    ExceptionHeadline.Offline -> KnownError.Offline
    ExceptionHeadline.Blocked -> KnownError.Blocked
    ExceptionHeadline.RateLimited -> KnownError.RateLimited
    ExceptionHeadline.NotFound -> KnownError.NotFound
    ExceptionHeadline.ServerError -> KnownError.ServerError
    else -> null
}

private const val HOUR_MS = 3_600_000L
private const val WEEK_MS = 7 * 24 * HOUR_MS

/** 24 hourly buckets; index 23 is the hour ending at [now]. Older than 24 h is ignored. */
fun hourlyCounts(times: List<Long>, now: Long): List<Int> {
    val counts = IntArray(24)
    times.forEach { t ->
        val age = now - t
        if (age in 0 until 24 * HOUR_MS) counts[23 - (age / HOUR_MS).toInt()]++
    }
    return counts.toList()
}

/** Group key → number of items in the last 7 days. */
fun seenCounts(items: List<Pair<Long, String>>, now: Long): Map<String, Int> =
    items.filter { (t, _) -> t >= now - WEEK_MS }.groupingBy { it.second }.eachCount()

private fun localDateTime(time: Long, timeZone: TimeZone) =
    Instant.fromEpochMilliseconds(time).toLocalDateTime(timeZone)

private fun LocalDateTime.format(): String =
    "$date ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
