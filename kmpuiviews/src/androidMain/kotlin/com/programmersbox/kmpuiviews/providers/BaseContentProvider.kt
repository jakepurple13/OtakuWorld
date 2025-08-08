package com.programmersbox.kmpuiviews.providers

import android.content.ContentProvider
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.programmersbox.kmpuiviews.logFirebaseMessage

abstract class BaseContentProvider : ContentProvider() {
    abstract val applicationId: String

    protected fun logWhoseCalling() {
        logFirebaseMessage("$callingPackage from ${this::class.java.name}")
        runCatching {
            Firebase.analytics.logEvent("content_provider") {
                param("provider", this@BaseContentProvider::class.java.name)
                param("package", callingPackage.orEmpty())
            }
        }
    }
}