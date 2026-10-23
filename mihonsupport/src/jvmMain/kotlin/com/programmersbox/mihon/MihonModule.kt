package com.programmersbox.mihon

import android.app.Application
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpextensionloader.ApkExtensionStore
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.mihon.repo.MihonRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

val mihonModule = module {
    single { MihonRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } }) }
    single {
        MihonApkStore(
            File(get<AppDirs>().getUserDataDir(), "mihon-extensions"),
            File(get<AppDirs>().getUserCacheDir(), "mihon-jars"),
        )
    } bind ApkExtensionStore::class
    single {
        MihonCatalog(
            Application("mihon", File(get<AppDirs>().getUserDataDir(), "mihon-data")),
            get<NewSettingsHandling>(),
            get(),
            get(),
        )
    } bind KmpExternalCustomApiServicesCatalog::class
}
