package com.programmersbox.aniyomi

import android.app.Application
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.aniyomi.repo.AniyomiRepoClient
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpextensionloader.ApkExtensionStore
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

val aniyomiModule = module {
    single { AniyomiRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } }) }
    single {
        AniyomiApkStore(
            File(get<AppDirs>().getUserDataDir(), "aniyomi-extensions"),
            File(get<AppDirs>().getUserCacheDir(), "aniyomi-jars"),
        )
    } bind ApkExtensionStore::class
    single {
        AniyomiCatalog(
            Application("aniyomi", File(get<AppDirs>().getUserDataDir(), "aniyomi-data")),
            get<NewSettingsHandling>(),
            get(),
            get(),
        )
    } bind KmpExternalCustomApiServicesCatalog::class
}
