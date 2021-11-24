package com.programmersbox.otakuworld

import android.app.Application
import com.google.android.material.color.DynamicColors

class PlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        //TODO: This acts funky if user enabled force dark mode from developer options
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}