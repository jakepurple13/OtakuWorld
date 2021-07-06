package com.programmersbox.animeworldtv

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AnimeWorldTV : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AnimeWorldTV)
            /*module {
                single { MainLogo(R.mipmap.ic_launcher) }
                single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
            }*/
        }
    }

}