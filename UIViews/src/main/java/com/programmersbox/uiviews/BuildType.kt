package com.programmersbox.uiviews

enum class BuildType {
    NoFirebase,
    NoCloudFirebase,
    Full;

    companion object {
        val current: BuildType by lazy {
            when (BuildConfig.FLAVOR) {
                "noFirebase" -> NoFirebase
                "noCloudFirebase" -> NoCloudFirebase
                else -> Full
            }
        }
    }
}