package com.programmersbox.uiviews

//TODO: Maybe move this to kmp and have the current as an extension value?
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