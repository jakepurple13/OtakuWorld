package com.programmersbox.sharedcomponents.qrcode

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual fun platformModule(): Module = module {
    singleOf(::QrCodeRepository)
}