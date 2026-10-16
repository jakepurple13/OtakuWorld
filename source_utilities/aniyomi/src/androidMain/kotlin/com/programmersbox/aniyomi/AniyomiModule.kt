package com.programmersbox.aniyomi

import com.programmersbox.aniyomi.repo.AniyomiRepoClient
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.bind
import org.koin.dsl.module

val aniyomiModule = module {
    single { AniyomiRepoClient(HttpClient(OkHttp) { engine { config { followSslRedirects(false) } } }) }
    single { AniyomiCatalog(get(), get(), get()) } bind KmpExternalCustomApiServicesCatalog::class
}
