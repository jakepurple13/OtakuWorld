package com.programmersbox.models

import com.tfowl.ktor.client.plugins.JsoupPlugin
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.gson.gson

fun createHttpClient(block: HttpClientConfig<OkHttpConfig>.() -> Unit = {}) = HttpClient(OkHttp) {
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        requestTimeoutMillis = 120_000
        socketTimeoutMillis = 30_000
    }
    install(JsoupPlugin)
    install(ContentNegotiation) { gson { setPrettyPrinting() } }
    block()
}
