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

class CatchUpViewModel(
    private val db: ItemDao,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val items = mutableStateListOf<NotificationItem>()

    var filter: NotificationFilter by mutableStateOf(NotificationFilter.All)
        private set

    private val pending = mutableStateListOf<PendingDeletion>()

    val selected = mutableStateSetOf<String>()

    var catchUp by mutableStateOf<CatchUp?>(null)
        private set

    private val visibleItems by derivedStateOf {
        val hidden = pending.flatMap { batch -> batch.items.map { it.url } }.toSet()
        items
            .filterNot { it.url in hidden }
            .sortedByDescending { it.createdAt }
    }

    val filteredItems by derivedStateOf { visibleItems.filter { it.matches(filter) } }

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
                catchUp = CatchUp(filteredItems.map { it.url })
            }
            .launchIn(viewModelScope)

        db.getAllNotificationCount()
            .filter { it == 0 }
            .onEach { notificationRepository.cancelGroup() }
            .launchIn(viewModelScope)
    }

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

    fun advanceCatchUp() {
        catchUp = catchUp?.let { it.copy(index = it.index + 1) }
    }

    fun stopConfirmingCatchUpDeletes() {
        catchUp = catchUp?.copy(confirmDeletes = false)
    }

    private fun resetFilterIfEmpty() {
        if (filter != NotificationFilter.All && visibleItems.none { it.matches(filter) }) {
            filter = NotificationFilter.All
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        val batches = pending.toList()
        if (batches.isEmpty()) return
        GlobalScope.launch { batches.forEach { delete(it) } }
    }
}