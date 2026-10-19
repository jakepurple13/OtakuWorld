// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
package com.lagradost.cloudstream3.utils

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import com.lagradost.cloudstream3.Prerelease
import com.lagradost.cloudstream3.mvvm.launchSafe
import com.lagradost.cloudstream3.mvvm.logError
import kotlinx.coroutines.*

// OTAKU: expect declarations removed; Coroutines.android.kt / Coroutines.jvmCommon.kt hold the
// (former) actuals for runOnMainThreadNative, workerDispatcher and WorkerThread.

object Coroutines {
    @AnyThread
    fun <T> T.main(@MainThread work: suspend ((T) -> Unit)): Job {
        val value = this
        return CoroutineScope(Dispatchers.Main).launchSafe {
            work(value)
        }
    }

    @AnyThread
    fun <T> T.ioSafe(@WorkerThread work: suspend (CoroutineScope.(T) -> Unit)): Job {
        val value = this
        return CoroutineScope(workerDispatcher).launchSafe {
            work(value)
        }
    }

    @AnyThread
    suspend fun <T, V> V.ioWorkSafe(@WorkerThread work: suspend (CoroutineScope.(V) -> T)): T? {
        val value = this
        return withContext(workerDispatcher) {
            try {
                work(value)
            } catch (e: Exception) {
                logError(e)
                null
            }
        }
    }

    @AnyThread
    suspend fun <T, V> V.ioWork(@WorkerThread work: suspend (CoroutineScope.(V) -> T)): T {
        val value = this
        return withContext(workerDispatcher) {
            work(value)
        }
    }

    @AnyThread
    suspend fun <T, V> V.mainWork(@MainThread work: suspend (CoroutineScope.(V) -> T)): T {
        val value = this
        return withContext(Dispatchers.Main) {
            work(value)
        }
    }

    @AnyThread
    fun runOnMainThread(@MainThread work: (() -> Unit)) {
        runOnMainThreadNative(work)
    }

    /**
     * Safe to add and remove how you want
     * If you want to iterate over the list then you need to do:
     * list.withLock { code here }
     */
    fun <T> atomicListOf(vararg items: T): AtomicMutableList<T> {
        return AtomicMutableList(items.toMutableList())
    }

    @Deprecated(
        message = "Use atomicListOf() instead.",
        replaceWith = ReplaceWith("atomicListOf(*items)"),
        level = DeprecationLevel.WARNING,
    )
    fun <T> threadSafeListOf(vararg items: T): MutableList<T> = atomicListOf(*items)
}
