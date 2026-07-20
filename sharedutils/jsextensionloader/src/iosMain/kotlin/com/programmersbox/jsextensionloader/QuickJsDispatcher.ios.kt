package com.programmersbox.jsextensionloader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// JS extension loading isn't wired up on iOS yet (see kmpuiviews - iosMain is stub-only),
// so there's no real QuickJs usage here to confine to a dedicated thread.
actual fun singleThreadQuickJsDispatcher(name: String): CoroutineDispatcher = Dispatchers.Default

actual fun closeQuickJsDispatcher(dispatcher: CoroutineDispatcher) = Unit
