package com.programmersbox.kmpuiviews.presentation.notifications

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

sealed interface NotificationFilter {
    data object All : NotificationFilter
    data object InTray : NotificationFilter
    data class Source(val name: String) : NotificationFilter
}

enum class NotificationDay { Today, Yesterday, ThisWeek, Older }

/** Items hidden from the list while the Undo snackbar is showing. */
class PendingDeletion(val items: List<NotificationItem>)

data class CatchUp(
    val urls: List<String>,
    val index: Int = 0,
    /** Whether a delete from the deck asks first. Resets for every new catch-up. */
    val confirmDeletes: Boolean = true,
)

class NotificationScreenViewModel(
    private val db: ItemDao,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val items = mutableStateListOf<NotificationItem>()

    var filter: NotificationFilter by mutableStateOf(NotificationFilter.All)
        private set

    val selected = mutableStateSetOf<String>()

    val isSelecting by derivedStateOf { selected.isNotEmpty() }

    private val pending = mutableStateListOf<PendingDeletion>()

    var catchUp by mutableStateOf<CatchUp?>(null)
        private set

    private val visibleItems by derivedStateOf {
        val hidden = pending.flatMap { batch -> batch.items.map { it.url } }.toSet()
        items
            .filterNot { it.url in hidden }
            .sortedByDescending { it.createdAt }
    }

    val filteredItems by derivedStateOf { visibleItems.filter { it.matches(filter) } }

    val totalCount by derivedStateOf { visibleItems.size }

    val trayCount by derivedStateOf { visibleItems.count { it.isShowing } }

    val sourceCounts by derivedStateOf {
        visibleItems
            .groupingBy { it.source }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    val dayBuckets by derivedStateOf {
        bucketByDay(filteredItems, Clock.System.now(), TimeZone.currentSystemDefault())
    }

    val catchUpRemaining by derivedStateOf {
        val state = catchUp ?: return@derivedStateOf emptyList()
        val byUrl = visibleItems.associateBy { it.url }
        state.urls.drop(state.index).mapNotNull { byUrl[it] }
    }

    init {
        db.getAllNotificationsFlow()
            .onEach {
                items.clear()
                items.addAll(it)
                resetFilterIfEmpty()
            }
            .launchIn(viewModelScope)

        db.getAllNotificationCount()
            .filter { it == 0 }
            .onEach { notificationRepository.cancelGroup() }
            .launchIn(viewModelScope)
    }

    fun updateFilter(filter: NotificationFilter) {
        this.filter = filter
        clearSelection()
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

    fun selectedItems(): List<NotificationItem> = filteredItems.filter { it.url in selected }

    /**
     * Hides [items] right away. The caller shows an Undo snackbar and then calls
     * [commitDeletion] or [undoDeletion] with the returned batch.
     */
    fun deleteWithUndo(items: List<NotificationItem>): PendingDeletion {
        val batch = PendingDeletion(items)
        pending.add(batch)
        items.forEach { selected.remove(it.url) }
        resetFilterIfEmpty()
        return batch
    }

    fun undoDeletion(batch: PendingDeletion) {
        pending.remove(batch)
    }

    fun commitDeletion(batch: PendingDeletion) {
        if (!pending.contains(batch)) return
        viewModelScope.launch {
            delete(batch)
            pending.remove(batch)
        }
    }

    private suspend fun delete(batch: PendingDeletion) {
        batch.items.forEach {
            withContext(Dispatchers.Default) { db.deleteNotification(it) }
            notificationRepository.cancelNotification(it)
        }
    }

    fun startCatchUp() {
        clearSelection()
        catchUp = CatchUp(filteredItems.map { it.url })
    }

    fun advanceCatchUp() {
        catchUp = catchUp?.let { it.copy(index = it.index + 1) }
    }

    fun stopConfirmingCatchUpDeletes() {
        catchUp = catchUp?.copy(confirmDeletes = false)
    }

    fun endCatchUp() {
        catchUp = null
    }

    suspend fun cancelNotificationById(id: Int) = notificationRepository.cancelById(id)

    suspend fun deleteAllNotifications(): Int {
        db.getAllNotifications().forEach { notificationRepository.cancelNotification(it) }
        notificationRepository.cancelGroup()
        return db.deleteAllNotifications()
    }

    private fun resetFilterIfEmpty() {
        if (filter != NotificationFilter.All && visibleItems.none { it.matches(filter) }) {
            filter = NotificationFilter.All
        }
    }

    // viewModelScope is already cancelled here, so finish the deletes the user didn't undo elsewhere.
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        val batches = pending.toList()
        if (batches.isEmpty()) return
        GlobalScope.launch { batches.forEach { delete(it) } }
    }
}

fun NotificationItem.matches(filter: NotificationFilter) = when (filter) {
    NotificationFilter.All -> true
    NotificationFilter.InTray -> isShowing
    is NotificationFilter.Source -> source == filter.name
}

/** Groups [items] by the local day they were saved on, newest first. Empty days are left out. */
fun bucketByDay(
    items: List<NotificationItem>,
    now: Instant,
    timeZone: TimeZone,
): List<Pair<NotificationDay, List<NotificationItem>>> {
    val today = now.toLocalDateTime(timeZone).date
    val yesterday = today.minus(DatePeriod(days = 1))
    val weekStart = today.minus(DatePeriod(days = 6))

    return items
        .sortedByDescending { it.createdAt }
        .groupBy { item ->
            if (item.createdAt <= 0L) return@groupBy NotificationDay.Older
            val date = Instant.fromEpochMilliseconds(item.createdAt).toLocalDateTime(timeZone).date
            when {
                date >= today -> NotificationDay.Today
                date == yesterday -> NotificationDay.Yesterday
                date >= weekStart -> NotificationDay.ThisWeek
                else -> NotificationDay.Older
            }
        }
        .toList()
        .sortedBy { it.first.ordinal }
}
