// Copied from recloudstream/cloudstream d9c55e2e5b020dde8e28c12a3e7802df962265ac, GPL-3.0
package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.InternalAPI
import kotlin.concurrent.Volatile

@InternalAPI
object AppDebug {
    @Volatile
    var isDebug: Boolean = false
}
