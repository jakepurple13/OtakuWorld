package com.programmersbox.kmpuiviews.presentation.history

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.RecentModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

sealed interface HistoryGroup {
    /** Saveable key for the group's sticky header. */
    val key: String

    data object Today : HistoryGroup {
        override val key = "today"
    }

    data object Yesterday : HistoryGroup {
        override val key = "yesterday"
    }

    data object ThisWeek : HistoryGroup {
        override val key = "this_week"
    }

    data class MonthOf(val year: Int, val month: Month) : HistoryGroup {
        override val key = "month_${year}_${month.ordinal}"
    }
}

/** Items hidden from the list while the Undo snackbar is showing. */
class PendingRemoval(val items: List<RecentModel>)

class HistoryViewModel(private val dao: HistoryDao) : ViewModel() {

    val items = mutableStateListOf<RecentModel>()

    var isLoading by mutableStateOf(true)
        private set

    var searchQuery by mutableStateOf("")
        private set

    /** The [RecentModel.source] to show, or null for all sources. */
    var sourceFilter by mutableStateOf<String?>(null)
        private set

    val selected = mutableStateSetOf<String>()

    val isSelecting by derivedStateOf { selected.isNotEmpty() }

    private val pending = mutableStateListOf<PendingRemoval>()

    private val visibleItems by derivedStateOf {
        val hidden = pending.flatMap { batch -> batch.items.map { it.url } }.toSet()
        items
            .filterNot { it.url in hidden }
            .sortedByDescending { it.timestamp }
    }

    val historyCount by derivedStateOf { visibleItems.size }

    val sources by derivedStateOf { visibleItems.map { it.source }.distinct() }

    val filteredItems by derivedStateOf {
        val query = searchQuery.trim()
        visibleItems.filter { item ->
            (sourceFilter == null || item.source == sourceFilter) &&
                    (query.isEmpty() || item.title.contains(query, ignoreCase = true))
        }
    }

    val groups by derivedStateOf {
        groupByDay(filteredItems, Clock.System.now(), TimeZone.currentSystemDefault())
    }

    init {
        dao.getRecentlyViewed()
            .onEach {
                items.clear()
                items.addAll(it)
                isLoading = false
                resetFilterIfEmpty()
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateSourceFilter(source: String?) {
        sourceFilter = source
    }

    fun resetSearchAndFilter() {
        searchQuery = ""
        sourceFilter = null
    }

    fun toggleSelection(url: String) {
        if (!selected.remove(url)) selected.add(url)
    }

    fun selectAll() {
        selected.addAll(filteredItems.map { it.url })
    }

    fun clearSelection() {
        selected.clear()
    }

    fun selectedItems(): List<RecentModel> = filteredItems.filter { it.url in selected }

    /**
     * Hides [items] right away. The caller shows an Undo snackbar and then calls
     * [commitRemoval] or [undoRemoval] with the returned batch.
     */
    fun removeWithUndo(items: List<RecentModel>): PendingRemoval {
        val batch = PendingRemoval(items)
        pending.add(batch)
        items.forEach { selected.remove(it.url) }
        resetFilterIfEmpty()
        return batch
    }

    fun undoRemoval(batch: PendingRemoval) {
        pending.remove(batch)
    }

    fun commitRemoval(batch: PendingRemoval) {
        if (!pending.contains(batch)) return
        viewModelScope.launch {
            delete(batch)
            pending.remove(batch)
        }
    }

    fun clearAll() {
        clearSelection()
        resetSearchAndFilter()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dao.deleteAllRecentHistory() }
            pending.clear()
        }
    }

    private suspend fun delete(batch: PendingRemoval) {
        withContext(Dispatchers.IO) { batch.items.forEach { dao.deleteRecent(it) } }
    }

    private fun resetFilterIfEmpty() {
        val source = sourceFilter ?: return
        if (visibleItems.none { it.source == source }) sourceFilter = null
    }

    // viewModelScope is already cancelled here, so finish the removals the user didn't undo elsewhere.
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        val batches = pending.toList()
        if (batches.isEmpty()) return
        GlobalScope.launch { batches.forEach { delete(it) } }
    }
}

/** Groups [items] by the local day they were opened on, newest first. Empty groups are left out. */
fun groupByDay(
    items: List<RecentModel>,
    now: Instant,
    timeZone: TimeZone,
): List<Pair<HistoryGroup, List<RecentModel>>> {
    val today = now.toLocalDateTime(timeZone).date
    val yesterday = today.minus(DatePeriod(days = 1))
    val weekStart = today.minus(DatePeriod(days = 6))

    return items
        .sortedByDescending { it.timestamp }
        .groupBy { item ->
            val date = Instant.fromEpochMilliseconds(item.timestamp).toLocalDateTime(timeZone).date
            when {
                date >= today -> HistoryGroup.Today
                date == yesterday -> HistoryGroup.Yesterday
                date >= weekStart -> HistoryGroup.ThisWeek
                else -> HistoryGroup.MonthOf(date.year, date.month)
            }
        }
        .toList()
}

/** "now", minutes, hours, or days under a week; month and day after that. */
fun relativeTimeLabel(timestamp: Long, now: Instant, timeZone: TimeZone): String {
    val elapsed = now - Instant.fromEpochMilliseconds(timestamp)
    return when {
        elapsed.inWholeMinutes < 1 -> "now"
        elapsed.inWholeHours < 1 -> "${elapsed.inWholeMinutes}m"
        elapsed.inWholeDays < 1 -> "${elapsed.inWholeHours}h"
        elapsed.inWholeDays < 7 -> "${elapsed.inWholeDays}d"
        else -> {
            val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)
            "${local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${local.day}"
        }
    }
}
