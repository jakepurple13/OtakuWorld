package com.programmersbox.kmpuiviews.presentation.quickactions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OpenOrigin { Bolt, Keyboard }

/** Holds whether the quick actions card is open, and what opened it. */
class QuickActionsController {
    private val _open = MutableStateFlow<OpenOrigin?>(null)
    val open: StateFlow<OpenOrigin?> = _open.asStateFlow()

    fun open(origin: OpenOrigin) {
        _open.value = origin
    }

    fun close() {
        _open.value = null
    }

    fun toggle(origin: OpenOrigin) {
        _open.value = if (_open.value == null) origin else null
    }
}
