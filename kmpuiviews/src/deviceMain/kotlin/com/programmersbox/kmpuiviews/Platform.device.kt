package com.programmersbox.kmpuiviews

import dev.jordond.connectivity.Connectivity

actual fun createConnectivityState(): Connectivity {
    return Connectivity {
        autoStart = true
    }
}