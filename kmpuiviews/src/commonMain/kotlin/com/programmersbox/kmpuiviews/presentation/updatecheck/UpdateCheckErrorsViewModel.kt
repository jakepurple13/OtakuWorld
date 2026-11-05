package com.programmersbox.kmpuiviews.presentation.updatecheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.updatecheck.FailureKind
import com.programmersbox.kmpuiviews.domain.updatecheck.ItemFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReportStore
import com.programmersbox.kmpuiviews.domain.updatecheck.failureMessage
import com.programmersbox.kmpuiviews.repository.WorkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.uc_failure_source_failed
import otakuworld.kmpuiviews.generated.resources.uc_share_header
import otakuworld.kmpuiviews.generated.resources.uc_share_summary
import otakuworld.kmpuiviews.generated.resources.uc_source_banner_title
import otakuworld.kmpuiviews.generated.resources.uc_stopped_early
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Failures of one source; [sourceFailure] is set when its recent list couldn't be loaded. */
data class FailureGroup(
    val source: String,
    val sourceFailure: SourceFailure?,
    val items: List<ItemFailure>,
    val dominantKind: FailureKind,
)

/** Groups by source: source-failure groups first, then by item count (most first), then by source. */
fun UpdateCheckReport.groupFailures(): List<FailureGroup> {
    val itemsBySource = itemFailures.groupBy { it.source }
    val sources = (sourceFailures.map { it.source } + itemsBySource.keys).distinct()
    return sources
        .map { source ->
            val sourceFailure = sourceFailures.firstOrNull { it.source == source }
            val items = itemsBySource[source].orEmpty()
            FailureGroup(
                source = source,
                sourceFailure = sourceFailure,
                items = items,
                dominantKind = sourceFailure?.kind
                    ?: items.groupingBy { it.kind }.eachCount().maxBy { it.value }.key,
            )
        }
        .sortedWith(
            compareBy<FailureGroup> { it.sourceFailure == null }
                .thenByDescending { it.items.size }
                .thenBy { it.source }
        )
}

/** One-line reason shown for an item. */
suspend fun ItemFailure.message(): String =
    if (sourceFailed) getString(Res.string.uc_failure_source_failed) else failureMessage(kind, httpCode)

private val shareDateTimeFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}

@OptIn(ExperimentalTime::class)
suspend fun UpdateCheckReport.toShareText(
    displayName: (String) -> String = { it },
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String = buildString {
    val finished = Instant.fromEpochMilliseconds(finishedAt).toLocalDateTime(timeZone)
    appendLine(getString(Res.string.uc_share_header, shareDateTimeFormat.format(finished)))
    append(getString(Res.string.uc_share_summary, favoriteCount, updatedCount, itemFailures.size))
    stoppedEarly?.let {
        appendLine()
        append(getString(Res.string.uc_stopped_early))
        if (it.isNotBlank()) append(" — $it")
    }
    groupFailures().forEachIndexed { index, group ->
        appendLine()
        if (index == 0) appendLine()
        val name = displayName(group.source)
        append(name)
        group.sourceFailure?.let {
            append(" — ")
            append(getString(Res.string.uc_source_banner_title, name))
            append(" (${failureMessage(it.kind, it.httpCode)})")
        }
        group.items.forEach { item ->
            appendLine()
            append("  ${item.title} — ${item.message()}")
            if (!item.sourceFailed && item.detail.isNotBlank()) {
                item.detail.lines().forEach {
                    appendLine()
                    append("    $it")
                }
            }
        }
    }
}

class UpdateCheckErrorsViewModel(
    private val store: UpdateCheckReportStore,
    private val workRepository: WorkRepository,
    private val sharer: UpdateCheckReportSharer,
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    private val loadedState = MutableStateFlow(false)

    /** False until the stored report has been read once, so the screen doesn't flash "no check yet". */
    val loaded: StateFlow<Boolean> = loadedState

    val report: StateFlow<UpdateCheckReport?> = store.report
        .onEach { loadedState.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val groups: StateFlow<List<FailureGroup>> = report
        .map { it?.groupFailures().orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val canRetry: StateFlow<Boolean> = report
        .map { it?.itemFailures?.isNotEmpty() == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun retryFailed() {
        workRepository.retryFailedUpdates()
    }

    fun share() {
        viewModelScope.launch {
            val report = store.current() ?: return@launch
            sharer.share(report.toShareText(displayName = sourceRepository::displayNameFor))
        }
    }
}
