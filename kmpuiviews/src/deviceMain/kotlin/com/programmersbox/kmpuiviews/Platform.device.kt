package com.programmersbox.kmpuiviews

import dev.jordond.connectivity.Connectivity

actual fun createConnectivity(): Connectivity {
    return Connectivity {
        autoStart = true
    }
}