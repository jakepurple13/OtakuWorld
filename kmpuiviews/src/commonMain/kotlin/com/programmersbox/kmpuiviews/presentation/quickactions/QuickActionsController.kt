package com.programmersbox.kmpuiviews.presentation.quickactions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OpenOrigin { Bolt, Keyboard }

/** Holds whether the quick actions card is open, and what opened it. */
class QuickActionsController {
    private val _open = MutableStateFlow<OpenOrigin?>(null)
    val open: StateFlow<OpenOrigin?> = _open.asStateFlow()

    private val _shortcutsOpen = MutableStateFlow(false)
    val shortcutsOpen: StateFlow<Boolean> = _shortcutsOpen.asStateFlow()

    fun open(origin: OpenOrigin) {
        _shortcutsOpen.value = false
        _open.value = origin
    }

    fun close() {
        _open.value = null
    }

    fun toggle(origin: OpenOrigin) {
        if (_open.value == null) open(origin) else close()
    }

    fun openShortcuts() {
        close()
        _shortcutsOpen.value = true
    }

    fun closeShortcuts() {
        _shortcutsOpen.value = false
    }
}
