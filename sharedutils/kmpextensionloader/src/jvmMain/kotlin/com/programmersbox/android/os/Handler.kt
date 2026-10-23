package android.os

import java.util.concurrent.TimeUnit

open class Handler(private val looper: Looper) {
    constructor() : this(Looper.myLooper() ?: Looper.getMainLooper())

    fun getLooper(): Looper = looper

    fun post(r: Runnable): Boolean = postDelayed(r, 0)

    fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
        looper.executor.schedule(r, delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
        return true
    }
}
