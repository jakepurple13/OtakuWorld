package com.programmersbox.sharedcomponents.qrcode

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun qrCodeModule() = module {
    viewModelOf(::QrCodeViewModel)
    includes(platformModule())
}

internal expect fun platformModule(): Module

@Serializable
data object QrCodeScanner : NavKey