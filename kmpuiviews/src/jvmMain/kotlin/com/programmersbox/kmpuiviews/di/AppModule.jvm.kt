package com.programmersbox.kmpuiviews.di

import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.PlatformDataStoreHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionShareHandler
import com.programmersbox.kmpuiviews.utils.ImageModifier
import com.programmersbox.kmpuiviews.utils.Zipper
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    singleOf(::DateTimeFormatHandler)
    singleOf(::IconLoader)
    singleOf(::PlatformDataStoreHandling)
    singleOf(::Zipper)
    singleOf(::ExtensionShareHandler)
    factoryOf(::ImageModifier)
    singleOf(::SystemAlerter)

    single {
        NewSettingsHandling(
            createProtobuf(
                serializer = SettingsSerializer()
            ),
        )
    }
}