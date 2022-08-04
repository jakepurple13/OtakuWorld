package com.programmersbox.models

import com.tfowl.ktor.client.features.JsoupPlugin
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*

fun createHttpClient(block: HttpClientConfig<OkHttpConfig>.() -> Unit = {}) = HttpClient(OkHttp) {
    install(JsoupPlugin)
    install(ContentNegotiation) { json() }
    install(Logging) {
        if (BuildConfig.DEBUG) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
    block()
}