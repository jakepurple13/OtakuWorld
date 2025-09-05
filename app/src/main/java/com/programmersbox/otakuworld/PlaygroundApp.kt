package com.programmersbox.otakuworld

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.programmersbox.otakuworld.info.InfoViewModel
import com.programmersbox.otakuworld.repository.OtakuRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class PlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        //TODO: This acts funky if user enabled force dark mode from developer options
        DynamicColors.applyToActivitiesIfAvailable(this)

        startKoin {
            androidContext(this@PlaygroundApp)
            loadKoinModules(
                module {
                    viewModelOf(::InfoViewModel)
                    singleOf(::OtakuProvider)
                    singleOf(::OtakuRepository)
                    singleOf(::AppInfo)
                }
            )
        }
    }
}