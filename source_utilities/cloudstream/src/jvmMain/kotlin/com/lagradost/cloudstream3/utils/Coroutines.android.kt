// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: desktop rewrite of the vendored androidMain Coroutines.android.kt (derived: same API). Same file name, so
// plugins find the same facade class (Coroutines_androidKt). Desktop has no Looper: the "main thread"
// is Dispatchers.Main when a Main dispatcher module (coroutines-swing) is present, else one daemon thread.
package com.lagradost.cloudstream3.utils

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.EmptyCoroutineContext

@AnyThread
fun runOnMainThreadNative(@MainThread work: () -> Unit) {
    if (isMainThread()) {
        // Already on the main thread: run inline, never wait on ourselves.
        work()
    } else {
        // Like Handler.post: enqueue and return.
        mainDispatcher.dispatch(EmptyCoroutineContext, Runnable(work))
    }
}

internal val mainDispatcher: CoroutineDispatcher
    get() = if (hasMainDispatcher) Dispatchers.Main else fallbackMain

private val hasMainDispatcher: Boolean by lazy {
    // Without a Main module, Dispatchers.Main throws on first use.
    runCatching { Dispatchers.Main.immediate.isDispatchNeeded(EmptyCoroutineContext) }.isSuccess
}

@Volatile
private var fallbackThread: Thread? = null

private val fallbackMain: CoroutineDispatcher by lazy {
    Executors.newSingleThreadExecutor { r ->
        Thread(r, "cloudstream-main").apply { isDaemon = true }.also { fallbackThread = it }
    }.asCoroutineDispatcher()
}

private fun isMainThread(): Boolean =
    if (hasMainDispatcher) !Dispatchers.Main.immediate.isDispatchNeeded(EmptyCoroutineContext)
    else Thread.currentThread() === fallbackThread
