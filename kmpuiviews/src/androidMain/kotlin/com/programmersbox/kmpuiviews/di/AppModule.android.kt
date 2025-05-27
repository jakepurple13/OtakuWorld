package com.programmersbox.kmpuiviews.di

import android.content.Context
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpuiviews.CustomUriHandler
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    singleOf(::DownloadAndInstaller)

    singleOf(::IconLoader)
    singleOf(::DateTimeFormatHandler)
    singleOf(::CustomUriHandler)

    single {
        NewSettingsHandling(
            createProtobuf(
                context = get<Context>(),
                serializer = SettingsSerializer()
            ),
        )
    }

    single {
        SourceLoader(
            application = get(),
            context = get(),
            sourceType = get<KmpGenericInfo>().sourceType,
            sourceRepository = get()
        )
    }
}