package com.programmersbox.kmpuiviews.di

import com.programmersbox.datastore.encrypted.EncryptedDataStoreFactory
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewScheduler
import com.programmersbox.kmpuiviews.domain.dailyoverview.NoOpDailyOverviewScheduler
import com.programmersbox.kmpuiviews.presentation.debug.navmap.NavGraphSource
import com.programmersbox.kmpuiviews.presentation.updatecheck.NoOpUpdateCheckReportSharer
import com.programmersbox.kmpuiviews.presentation.updatecheck.UpdateCheckReportSharer
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    singleOf(::EncryptedDataStoreFactory)
    singleOf(::NoOpUpdateCheckReportSharer) { bind<UpdateCheckReportSharer>() }
    single<DailyOverviewScheduler> { NoOpDailyOverviewScheduler }
    single<NavGraphSource> {
        object : NavGraphSource {
            override suspend fun read(): String? = null
            override val available = false
        }
    }
}