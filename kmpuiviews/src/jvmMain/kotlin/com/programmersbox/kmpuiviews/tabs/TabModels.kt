package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@JvmInline
value class TabId(val v: Long)

@JvmInline
value class WindowId(val v: Long)

class TabViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

class TabState(
    val id: TabId,
    val nav: TabNavigation3Actions,
    val pinned: Boolean,
) {
    val notesOpen: MutableState<Boolean> = mutableStateOf(false)
    val playing: MutableState<Boolean> = mutableStateOf(false)

    /** Asleep: its ViewModels and saved UI state are gone, and it reloads when next shown. */
    val sleeping: MutableState<Boolean> = mutableStateOf(false)
    val viewModelStoreOwner: ViewModelStoreOwner = TabViewModelStoreOwner()

    /**
     * Holds the tab's page ViewModelStores. It lives outside composition, so a parked tab keeps its
     * ViewModels; clearing [viewModelStoreOwner] on close clears them. Sleeping replaces it: the old one
     * keeps its cleared state holder, which is no longer in [viewModelStoreOwner]'s store.
     */
    var viewModelStoreProvider: ViewModelStoreProvider = ViewModelStoreProvider(parentOwner = viewModelStoreOwner)
        internal set

    val current: NavKey get() = nav.history.current
}

@Serializable
data class WindowBounds(
    @Serializable(with = DpSerializer::class) val x: Dp,
    @Serializable(with = DpSerializer::class) val y: Dp,
    @Serializable(with = DpSerializer::class) val width: Dp,
    @Serializable(with = DpSerializer::class) val height: Dp,
)

class BrowserWindowState(
    val id: WindowId,
    val isMain: Boolean,
    first: TabState,
    bounds: WindowBounds? = null,
) {
    val tabs: SnapshotStateList<TabState> = mutableStateListOf(first)
    val activeId: MutableState<TabId> = mutableStateOf(first.id)
    val bounds: MutableState<WindowBounds?> = mutableStateOf(bounds)
}

sealed interface TabEvent {
    data class Opened(val tab: TabId, val window: WindowId, val mode: TabOpenMode, val origin: Rect?) : TabEvent
    data class Flash(val tab: TabId) : TabEvent
    data class Closed(val tab: TabId, val window: WindowId) : TabEvent
    data class Moved(val tab: TabId, val from: WindowId, val to: WindowId) : TabEvent
    data class Slept(val tab: TabId, val window: WindowId) : TabEvent
}

data class TabPrefsSnapshot(
    val clickOpensNewTab: Boolean,
    val modClick: ModClick = ModClick.Background,
    val newPlace: NewTabPlace = NewTabPlace.NextToCurrent,
    val afterClose: AfterClose = AfterClose.Right,
    val closedKept: Int = 25,
    /** "Drag tabs out into windows". Off: dragging only reorders, and nothing opens a new window. */
    val tearOut: Boolean = true,
)

/** A closed tab: its history, the position in it, and what the [TabHandoff]s held for it. */
class ClosedTab(val entries: List<NavKey>, val index: Int, internal val handoffs: List<Any?>?) {
    val current: NavKey get() = entries[index]
}

/** A new tab page nothing has navigated from yet. */
val TabState.isUntouchedNewTab: Boolean get() = current == NewTabRoute && nav.history.entries.size == 1
