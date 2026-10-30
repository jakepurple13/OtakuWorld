package com.programmersbox.kmpuiviews.updates

enum class UpdateTrack {
    Stable,
    Nightly;

    companion object {
        /** A `-` in the version (e.g. `33.0.0-alpha.20261030.1415`) marks a nightly build. */
        fun fromVersion(version: String): UpdateTrack = if ('-' in version) Nightly else Stable
    }
}
