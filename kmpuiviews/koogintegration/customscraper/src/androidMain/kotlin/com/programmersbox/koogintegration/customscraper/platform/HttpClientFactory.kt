package com.programmersbox.koogintegration.customscraper.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

// Android engine — wraps OkHttp with Android-specific socket/timeout defaults.
actual fun createHttpClient(): HttpClient = HttpClient(Android)
