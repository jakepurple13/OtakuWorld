package com.programmersbox.otakuworld

import android.content.Context

class AppInfo(
    val context: Context,
) {
    val provider = when (BuildConfig.FLAVOR) {
        "noFirebase" -> Provider.NoFirebase
        "noCloudFirebase" -> Provider.NoCloudFirebase
        else -> Provider.Full
    }
}