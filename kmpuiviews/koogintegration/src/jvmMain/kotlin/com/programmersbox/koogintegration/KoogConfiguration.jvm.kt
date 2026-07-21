package com.programmersbox.koogintegration

import ca.gosyer.appdirs.AppDirs
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single {
        ModelManager(
            client = HttpClient(),
            cacheDirectoryPath = get<AppDirs>().getUserDataDir()
        )
    }

    singleOf(::PlatformAgents)
}