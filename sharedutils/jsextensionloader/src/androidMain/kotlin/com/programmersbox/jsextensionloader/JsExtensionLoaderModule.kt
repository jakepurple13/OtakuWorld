package com.programmersbox.jsextensionloader

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import org.koin.dsl.module

/**
 * Not loaded by this plan. A consuming app wires this in with
 * `loadKoinModules(jsExtensionLoaderModule)` plus `JsExtensionUpdateScheduler.schedule(...)`
 * once it decides to integrate JS/TS extensions — that integration is out of scope here.
 */
val jsExtensionLoaderModule = module {
    single { HttpClient(Android) }
    single { KtorHostBridge(get()) }
    single<HostBridge> { get<KtorHostBridge>() }
    single { JSExtensionLoader(get()) }
    single { JsExtensionRepository() }
    single { ExtensionUpdateChecker(get()) }
    single { JsExtensionUpdateSettings() }
    single {
        ExtensionDiscovery(
            context = get(),
            extensionsSubDir = "js_extensions",
            bundledAssetsDir = "js_extensions",
            client = get(),
        )
    }
}
