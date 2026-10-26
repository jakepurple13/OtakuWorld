// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: `actual` modifiers removed; the commonMain `expect` is not vendored.
package com.lagradost.api

import android.content.Context
import java.lang.ref.WeakReference

private var contextRef: WeakReference<Context>? = null

/**
 * Helper function for Android specific context. Not usable in JVM.
 * Do not use this unless absolutely necessary.
 */
fun getContext(): Any? = contextRef?.get()

fun setContext(context: Any?) {
    contextRef = (context as? Context)?.let { WeakReference(it) }
}
