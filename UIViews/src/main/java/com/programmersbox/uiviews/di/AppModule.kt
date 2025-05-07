package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.di.appModule
import com.programmersbox.sharedutils.FirebaseUIStyle
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.checkers.UpdateNotification
import com.programmersbox.uiviews.datastore.OtakuDataStoreHandling
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.utils.PerformanceClass
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf

fun Module.appModule() {
    single { FirebaseUIStyle(R.style.Theme_OtakuWorldBase) }

    single { PerformanceClass.create() }
    singleOf(::UpdateNotification)
    singleOf(::OtakuDataStoreHandling)
    singleOf(::SettingsHandling)

    includes(appModule)
}