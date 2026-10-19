package com.programmersbox.cloudstream

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.CommonActivity

/**
 * Wires the CloudStream app-module shims to the host app. Call once from Application.onCreate,
 * before any plugin loads: plugins read CloudStreamApp.context and CommonActivity.activity.
 */
object CloudstreamShims {
    fun install(application: Application) {
        CloudStreamApp.context = application
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) =
                    CommonActivity.setActivityInstance(activity)

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }
}
