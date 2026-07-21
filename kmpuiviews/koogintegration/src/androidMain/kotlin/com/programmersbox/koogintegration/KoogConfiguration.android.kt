package com.programmersbox.koogintegration

import android.content.Context
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    single {
        ModelManager(
            client = HttpClient(),
            cacheDirectoryPath = File(get<Context>().filesDir, "models").absolutePath
        )
    }

    singleOf(::PlatformAgents)
}