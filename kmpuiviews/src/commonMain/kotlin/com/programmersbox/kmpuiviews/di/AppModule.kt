package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.domain.AppUpdateCheck
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::AppUpdateCheck)

    includes(platformModule())
}

expect fun platformModule(): Module