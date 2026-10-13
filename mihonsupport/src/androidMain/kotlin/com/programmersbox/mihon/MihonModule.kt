package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.mihon.repo.MihonRepoClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.bind
import org.koin.dsl.module

val mihonModule = module {
    single { MihonRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } }) }
    single { MihonCatalog(get(), get(), get()) } bind KmpExternalCustomApiServicesCatalog::class
}
