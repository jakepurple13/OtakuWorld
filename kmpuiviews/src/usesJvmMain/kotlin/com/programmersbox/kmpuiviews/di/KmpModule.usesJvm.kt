package com.programmersbox.kmpuiviews.di

import com.programmersbox.koogintegration.buildKoogModule
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun buildPlatformModule(): Module = module {
    includes(
        buildKoogModule()
    )
}