package com.programmersbox.kmpuiviews.di

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.OtakuWorldCatalog
import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::AppUpdateCheck)
    single {
        OtakuWorldCatalog(
            get<KmpGenericInfo>().sourceType
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        )
    }

    singleOf(::DataStoreHandling)

    includes(platformModule())
}

expect fun platformModule(): Module