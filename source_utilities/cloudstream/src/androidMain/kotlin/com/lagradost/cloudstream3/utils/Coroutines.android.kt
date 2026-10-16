// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: `actual` modifiers removed; the commonMain `expect` is not vendored.
package com.lagradost.cloudstream3.utils

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.annotation.AnyThread
import androidx.annotation.MainThread

@SuppressLint("ThreadConstraint") // mainLooper.isCurrentThread does not switch the context
@AnyThread
fun runOnMainThreadNative(@MainThread work: () -> Unit) {
    val mainLooper = Looper.getMainLooper()
    if (mainLooper.isCurrentThread) {
        // Do the work directly if we already are on the main thread, no need to enqueue it
        work()
    } else {
        // Otherwise post it to the other main thread
        Handler(mainLooper).post(work)
    }
}
