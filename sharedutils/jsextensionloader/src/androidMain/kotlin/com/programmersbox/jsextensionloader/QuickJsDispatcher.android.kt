package com.programmersbox.jsextensionloader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.newSingleThreadContext

actual fun singleThreadQuickJsDispatcher(name: String): CoroutineDispatcher = newSingleThreadContext(name)

actual fun closeQuickJsDispatcher(dispatcher: CoroutineDispatcher) {
    (dispatcher as? ExecutorCoroutineDispatcher)?.close()
}
