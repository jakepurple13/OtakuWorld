package com.programmersbox.cloudstream

import android.app.Application
import com.lagradost.cloudstream3.CloudStreamApp

/**
 * Desktop counterpart of the Android shims: plugins read CloudStreamApp.context. There are no activities,
 * so CommonActivity.activity stays null and no lifecycle callbacks are registered.
 */
object CloudstreamShims {
    fun install(application: Application) {
        CloudStreamApp.context = application
    }
}
