package com.programmersbox.models

import com.tfowl.ktor.client.features.JsoupPlugin
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.gson.gson

fun createHttpClient(block: HttpClientConfig<OkHttpConfig>.() -> Unit = {}) = HttpClient(OkHttp) {
    install(JsoupPlugin)
    install(ContentNegotiation) { gson { setPrettyPrinting() } }
    /*install(Logging) {
        if (BuildConfig.DEBUG) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }*/
    block()
}