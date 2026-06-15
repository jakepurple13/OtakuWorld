package com.programmersbox.koogintegration.customscraper.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

// OkHttp engine — cross-platform, works on Desktop JVM without Android runtime.
// No CIO engine is present in the project's version catalog, so OkHttp is used here.
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)
