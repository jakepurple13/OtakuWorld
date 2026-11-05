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
 */
fun UpdateCheckReport.mergeRetry(
    retry: UpdateCheckReport,
    retriedUrls: Set<String>,
    retriedSources: Set<String>,
): UpdateCheckReport = copy(
    updatedCount = updatedCount + retry.updatedCount,
    sourceFailures = sourceFailures.filterNot { it.source in retriedSources } + retry.sourceFailures,
    itemFailures = itemFailures.filterNot { it.url in retriedUrls } + retry.itemFailures,
    stoppedEarly = retry.stoppedEarly,
    retriedAt = retry.finishedAt,
)
