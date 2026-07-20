package com.programmersbox.kmpuiviews.di

import androidx.datastore.preferences.core.stringPreferencesKey
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.koogintegration.KoogDataStore
import com.programmersbox.koogintegration.buildKoogModule
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun buildPlatformModule(): Module = module {

    includes(buildKoogModule())

    single {
        val koogApiKey = DataStoreHandler(
            key = stringPreferencesKey("koogApiKey"),
            defaultValue = ""
        )

        val koogCompany = DataStoreHandler(
            key = stringPreferencesKey("koogCompany"),
            defaultValue = ""
        )

        val koogModel = DataStoreHandler(
            key = stringPreferencesKey("koogModel"),
            defaultValue = ""
        )

        KoogDataStore(
            getApiKey = { koogApiKey.get() },
            getModelCompany = { koogCompany.get() },
            getModelName = { koogModel.get() },
            storeApiKey = { koogApiKey.set(it) },
            storeModelCompany = { koogCompany.set(it) },
            storeModelName = { koogModel.set(it) },
            apiKeyFlow = koogApiKey.asFlow(),
            modelCompanyFlow = koogCompany.asFlow(),
            modelNameFlow = koogModel.asFlow()
        )
    }
}