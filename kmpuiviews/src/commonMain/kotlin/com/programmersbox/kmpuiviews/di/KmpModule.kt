package com.programmersbox.kmpuiviews.di

import org.koin.core.module.Module
import org.koin.dsl.module

val kmpModule = module {
    includes(
        appModule,
        databases,
        dictionaryModule,
        repositories,
        viewModels,
        aiModule,
        navigationModule,
        buildPlatformModule()
    )
}

expect fun buildPlatformModule(): Module
