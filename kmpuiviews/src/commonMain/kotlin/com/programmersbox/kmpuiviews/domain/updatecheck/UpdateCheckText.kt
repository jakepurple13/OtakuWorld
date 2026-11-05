package com.programmersbox.kmpuiviews.domain.updatecheck

import com.programmersbox.kmpmodels.KmpChapterModel
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.uc_failure_http
import otakuworld.kmpuiviews.generated.resources.uc_failure_http_cloudflare
import otakuworld.kmpuiviews.generated.resources.uc_failure_timeout
import otakuworld.kmpuiviews.generated.resources.uc_failure_unreadable
import otakuworld.kmpuiviews.generated.resources.uc_fallback_title
import otakuworld.kmpuiviews.generated.resources.uc_finished_stopped
import otakuworld.kmpuiviews.generated.resources.uc_finished_text
import otakuworld.kmpuiviews.generated.resources.uc_finished_title
import otakuworld.kmpuiviews.generated.resources.uc_finished_title_none
import otakuworld.kmpuiviews.generated.resources.uc_headline
import otakuworld.kmpuiviews.generated.resources.uc_source_failed
import otakuworld.kmpuiviews.generated.resources.uc_source_no_updates
import otakuworld.kmpuiviews.generated.resources.uc_source_pending
import otakuworld.kmpuiviews.generated.resources.uc_source_updates
import otakuworld.kmpuiviews.generated.resources.uc_summary_items_failed
import otakuworld.kmpuiviews.generated.resources.uc_summary_source_failed
import otakuworld.kmpuiviews.generated.resources.uc_summary_updates
import otakuworld.kmpuiviews.generated.resources.uc_title_chapters
import otakuworld.kmpuiviews.generated.resources.uc_title_episodes
import kotlin.math.max

/** What a source calls its units; only changes notification wording. */
enum class UpdateUnit { Chapters, Episodes }

suspend fun updateTitle(title: String, count: Int, unit: UpdateUnit): String = getPluralString(
    when (unit) {
        UpdateUnit.Chapters -> Res.plurals.uc_title_chapters
        UpdateUnit.Episodes -> Res.plurals.uc_title_episodes
    },
    count,
    title,
    count,
)

/** [newChapters] is newest first. */
fun chapterRange(newChapters: List<KmpChapterModel>): String = when (newChapters.size) {
    0 -> ""
    1 -> newChapters.first().name
    else -> "${newChapters.last().name} – ${newChapters.first().name}"
}

fun inboxLines(newChapters: List<KmpChapterModel>, max: Int = 5): List<String> {
    val lines = newChapters.take(max).map { it.name }
    val rest = newChapters.size - max
    return if (rest > 0) lines + "+$rest more" else lines
}

suspend fun UpdateCheckProgress.headline(): String =
    getString(Res.string.uc_headline, sources.size, favoriteCount)

suspend fun UpdateCheckProgress.fallbackTitle(): String =
    if (loadingRecents) headline() else getString(Res.string.uc_fallback_title, current.orEmpty(), checked, total)

suspend fun UpdateCheckProgress.summary(): String {
    val parts = buildList {
        if (updatesFound > 0) add(getPluralString(Res.plurals.uc_summary_updates, updatesFound, updatesFound))
        sources.filter { it.state == SourceState.Failed }
            .forEach { add(getString(Res.string.uc_summary_source_failed, it.name)) }
        if (itemFailures > 0) add(getPluralString(Res.plurals.uc_summary_items_failed, itemFailures, itemFailures))
    }
    return parts.joinToString(" · ")
}

suspend fun UpdateCheckProgress.sourceLines(): List<String> = sources.map {
    when (it.state) {
        SourceState.Done -> when (it.updates) {
            0 -> getString(Res.string.uc_source_no_updates, it.name)
            else -> getPluralString(Res.plurals.uc_source_updates, it.updates, it.name, it.updates)
        }

        SourceState.Failed -> getString(
            Res.string.uc_source_failed,
            it.name,
            failureMessage(it.failure?.kind ?: FailureKind.CouldNotRead, it.failure?.httpCode),
        )

        SourceState.Pending, SourceState.Running -> getString(Res.string.uc_source_pending, it.name)
    }
}

suspend fun UpdateCheckReport.finishedTitle(): String =
    if (updatedCount > 0) {
        getPluralString(Res.plurals.uc_finished_title, updatedCount, updatedCount, favoriteCount)
    } else {
        getString(Res.string.uc_finished_title_none, favoriteCount)
    }

suspend fun UpdateCheckReport.finishedText(): String? = when {
    !hasProblems -> null
    stoppedEarly != null -> getString(Res.string.uc_finished_stopped)
    else -> {
        val count = itemFailures.size + sourceFailures.size
        getPluralString(Res.plurals.uc_finished_text, count, count)
    }
}

suspend fun failureMessage(kind: FailureKind, httpCode: Int?): String = when (kind) {
    FailureKind.TimedOut -> getString(Res.string.uc_failure_timeout)
    FailureKind.Http -> when (httpCode) {
        null -> getString(Res.string.uc_failure_unreadable)
        403, 503 -> getString(Res.string.uc_failure_http_cloudflare, httpCode)
        else -> getString(Res.string.uc_failure_http, httpCode)
    }

    FailureKind.CouldNotRead -> getString(Res.string.uc_failure_unreadable)
}

/** (length, failed) per source, overall progress and the offsets where sources found updates. */
data class SegmentLayout(
    val segments: List<Pair<Int, Boolean>>,
    val progress: Int,
    val points: List<Int>,
)

fun UpdateCheckProgress.segmentLayout(): SegmentLayout {
    var offset = 0
    var progress = 0
    val points = mutableListOf<Int>()
    val segments = sources.map {
        val length = max(1, it.favorites)
        progress += when (it.state) {
            SourceState.Done, SourceState.Failed -> length
            SourceState.Running -> if (it.toCheck > 0) length * it.checked / it.toCheck else 0
            SourceState.Pending -> 0
        }
        offset += length
        if (it.updates > 0) points += offset
        length to (it.state == SourceState.Failed)
    }
    return SegmentLayout(segments, progress, points)
}
