package com.programmersbox.kmpuiviews.di

import org.koin.dsl.module

val kmpModule = module {
    includes(
        appModule,
        databases,
        repositories,
        viewModels,
        aiModule
    )
}