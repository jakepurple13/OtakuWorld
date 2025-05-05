package com.programmersbox.kmpuiviews

import dev.jordond.connectivity.Connectivity

actual fun createConnectivityState(): Connectivity {
    return Connectivity {
        autoStart = true
        urls("cloudflare.com", "my-own-domain.com")
        port = 80
        pollingIntervalMs = 10.minutes
        timeoutMs = 5.seconds
    }
}