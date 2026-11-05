package com.programmersbox.kmpuiviews.domain.updatecheck

import kotlinx.serialization.Serializable

@Serializable
enum class FailureKind { TimedOut, Http, CouldNotRead }

@Serializable
data class SourceFailure(
    val source: String,
    val kind: FailureKind,
    val httpCode: Int?,
    val detail: String,
)

@Serializable
data class ItemFailure(
    val url: String,
    val title: String,
    val imageUrl: String,
    val source: String,
    val kind: FailureKind,
    val httpCode: Int?,
    val detail: String,
    val sourceFailed: Boolean,
)

@Serializable
data class UpdateCheckReport(
    val startedAt: Long,
    val finishedAt: Long,
    val favoriteCount: Int,
    val checkedCount: Int,
    val updatedCount: Int,
    val sourceFailures: List<SourceFailure>,
    val itemFailures: List<ItemFailure>,
    val stoppedEarly: String? = null,
    val retriedAt: Long? = null,
) {
    val hasProblems: Boolean
        get() = itemFailures.isNotEmpty() || sourceFailures.isNotEmpty() || stoppedEarly != null
}

/**
 * Folds the result of a retry run into this report: failures for retried items/sources are replaced
 * by whatever the retry reported, everything else is kept.
 * A retry that stopped early didn't get to every retried item, so then only the failures it reported replace
 * earlier ones (by url / source) and every other earlier failure is kept.
 */
fun UpdateCheckReport.mergeRetry(
    retry: UpdateCheckReport,
    retriedUrls: Set<String>,
    retriedSources: Set<String>,
): UpdateCheckReport {
    val stopped = retry.stoppedEarly != null
    val replacedUrls = if (stopped) retry.itemFailures.mapTo(HashSet()) { it.url } else retriedUrls
    val replacedSources = if (stopped) retry.sourceFailures.mapTo(HashSet()) { it.source } else retriedSources
    return copy(
        updatedCount = updatedCount + retry.updatedCount,
        sourceFailures = sourceFailures.filterNot { it.source in replacedSources } + retry.sourceFailures,
        itemFailures = itemFailures.filterNot { it.url in replacedUrls } + retry.itemFailures,
        stoppedEarly = retry.stoppedEarly ?: stoppedEarly,
        retriedAt = retry.finishedAt,
    )
}
