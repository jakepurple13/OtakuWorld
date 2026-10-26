// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
// OTAKU: `actual` modifiers removed; the commonMain `expect` is not vendored.
package com.lagradost.cloudstream3.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

val workerDispatcher: CoroutineDispatcher = Dispatchers.IO
internal typealias WorkerThread = androidx.annotation.WorkerThread
