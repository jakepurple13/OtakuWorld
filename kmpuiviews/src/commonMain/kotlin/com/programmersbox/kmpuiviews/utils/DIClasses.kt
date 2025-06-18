package com.programmersbox.kmpuiviews.utils

import com.programmersbox.kmpuiviews.BuildType

class AppConfig(
    val appName: String,
    val buildType: BuildType,
    val isDebug: Boolean,
) {
    companion object {
        var forLaterUuid: String? = null
    }
}