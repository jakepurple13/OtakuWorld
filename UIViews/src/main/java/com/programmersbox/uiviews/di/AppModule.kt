package com.programmersbox.uiviews.di

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.sharedutils.FirebaseUIStyle
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.OtakuWorldCatalog
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.checkers.UpdateNotification
import com.programmersbox.uiviews.datastore.OtakuDataStoreHandling
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadAndInstaller
import com.programmersbox.uiviews.utils.PerformanceClass
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import java.util.Locale

fun Module.appModule() {
    single { FirebaseUIStyle(R.style.Theme_OtakuWorldBase) }

    single { PerformanceClass.create() }
    single { UpdateNotification(get()) }
    singleOf(::DataStoreHandling)
    singleOf(::SettingsHandling)
    singleOf(::OtakuDataStoreHandling)
    singleOf(::DownloadAndInstaller)

    single {
        //val performanceClass = get<PerformanceClass>()
        NewSettingsHandling(
            createProtobuf(
                context = get(),
                serializer = SettingsSerializer(true)
            ),
            canShowBlur = true
        )
    }

    single {
        OtakuWorldCatalog(
            get<GenericInfo>().sourceType
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        )
    }
}