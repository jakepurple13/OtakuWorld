package com.programmersbox.jsextensionloader

import ca.gosyer.appdirs.AppDirs
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import java.io.File

private val jsExtensionLoaderHttpJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

val jsExtensionLoaderModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(jsExtensionLoaderHttpJson) }
        }
    }
    single<HostBridge> { KtorHostBridge(get()) }
    single { JSExtensionLoader(get()) }
    single { JsExtensionRepository() }
    single { ExtensionUpdateChecker(get()) }
    single { JsExtensionUpdateSettings() }
    single {
        ExtensionDiscovery(
            extensionsDir = { File(get<AppDirs>().getUserDataDir(), "js_extensions") },
            bundledResourcesDir = "js_extensions",
            client = get(),
        )
    }
}
