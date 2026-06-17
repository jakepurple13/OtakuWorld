package com.programmersbox.koogintegration.customscraper.platform

import io.ktor.client.HttpClient

// Platform-specific engine selected at compile time via expect/actual.
// Android uses the Android (OkHttp) engine; JVM Desktop uses OkHttp directly.
expect fun createHttpClient(): HttpClient

//expect fun fetchRenderedHtml(url: String): String