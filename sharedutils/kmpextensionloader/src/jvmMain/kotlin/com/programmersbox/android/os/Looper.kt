package android.os

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/** One daemon thread stands in for Android's main looper. */
class Looper private constructor(internal val executor: ScheduledExecutorService, private val thread: Thread) {
    fun getThread(): Thread = thread
    fun isCurrentThread(): Boolean = Thread.currentThread() == thread

    companion object {
        private val main: Looper by lazy {
            var created: Thread? = null
            val executor = Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "android-main").apply { isDaemon = true }.also { created = it }
            }
            executor.submit {}.get() // start the thread so it exists before anyone compares against it
            Looper(executor, created!!)
        }

        @JvmStatic fun getMainLooper(): Looper = main

        /** Like Android, null on threads without a looper; only the main thread has one. */
        @JvmStatic fun myLooper(): Looper? = main.takeIf { it.isCurrentThread() }
    }
}
