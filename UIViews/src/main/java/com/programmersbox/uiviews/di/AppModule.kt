package com.programmersbox.uiviews.di

import android.app.Application
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.FirebaseUIStyle
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.OtakuDataStoreHandling
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.utils.PerformanceClass
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModules = module {
    single { FirebaseUIStyle(R.style.Theme_OtakuWorldBase) }

    single {
        val application = get<Application>()
        val applicationInfo = application.applicationInfo
        val packageManager = application.packageManager
        AppLogo(
            logo = applicationInfo.loadIcon(packageManager),
            logoId = applicationInfo.icon
        )
    }
    single {
        val application = get<Application>()
        val applicationInfo = application.applicationInfo
        val packageManager = application.packageManager
        com.programmersbox.kmpuiviews.utils.AppLogo(
            logo = applicationInfo.loadIcon(packageManager),
            logoId = applicationInfo.icon
        )
    }

    single { PerformanceClass.create() }
    singleOf(::OtakuDataStoreHandling)
    singleOf(::SettingsHandling)
    includes(repository)
}