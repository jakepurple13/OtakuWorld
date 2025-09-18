package com.programmersbox.otakuworld

import android.content.Context
import com.programmersbox.otakuworld.providers.Provider

class AppInfo(
    val context: Context,
) {
    val provider = when (BuildConfig.FLAVOR) {
        "noFirebase" -> Provider.NoFirebase
        "noCloudFirebase" -> Provider.NoCloudFirebase
        else -> Provider.Full
    }
}