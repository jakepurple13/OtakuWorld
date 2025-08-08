package com.programmersbox.otakuworld

import android.app.Application
import com.google.android.material.color.DynamicColors
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        //TODO: This acts funky if user enabled force dark mode from developer options
        DynamicColors.applyToActivitiesIfAvailable(this)

        startKoin {
            androidContext(this@PlaygroundApp)
            /*loadKoinModules(
                module {
                    single { NetworkHelper(get()) }
                }
            )*/
        }
    }
}