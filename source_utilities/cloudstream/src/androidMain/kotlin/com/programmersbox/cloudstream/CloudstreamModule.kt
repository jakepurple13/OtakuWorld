package com.programmersbox.cloudstream

import android.app.Application
import com.programmersbox.cloudstream.plugins.PluginLoader
import com.programmersbox.cloudstream.plugins.PluginStore
import com.programmersbox.cloudstream.repo.CloudstreamRepoClient
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

val cloudstreamModule = module {
    single { CloudstreamRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } }) }
    single {
        PluginStore(
            root = File(get<Application>().filesDir, "cloudstream"),
            // Plugin files are executed; do not follow an https -> http redirect.
            client = OkHttpClient.Builder().followSslRedirects(false).build(),
        )
    }
    single { PluginLoader(get<Application>()) }
    single { CloudstreamCatalog(get(), get(), get(), get(), get()) } bind KmpExternalCustomApiServicesCatalog::class
}
