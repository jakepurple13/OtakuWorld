package com.programmersbox.cloudstream

import android.app.Application
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.cloudstream.plugins.PluginLoader
import com.programmersbox.cloudstream.plugins.PluginStore
import com.programmersbox.cloudstream.repo.CloudstreamRepoClient
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

/** The desktop stand-in for the Android Application: plugins receive it as their Context. */
private val cloudstreamApplication = named("cloudstreamApplication")

val cloudstreamModule = module {
    single { CloudstreamRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } }) }
    single(cloudstreamApplication) {
        Application("cloudstream", File(get<AppDirs>().getUserDataDir(), "cloudstream-data"))
    }
    single {
        PluginStore(
            root = File(get<AppDirs>().getUserDataDir(), "cloudstream-plugins"),
            // Plugin files are executed; do not follow an https -> http redirect.
            client = OkHttpClient.Builder().followSslRedirects(false).build(),
        )
    }
    single {
        PluginLoader(File(get<AppDirs>().getUserCacheDir(), "cloudstream-jars"), get<Application>(cloudstreamApplication))
    }
    single {
        CloudstreamCatalog(get<Application>(cloudstreamApplication), get(), get(), get(), get())
    } bind KmpExternalCustomApiServicesCatalog::class
}
