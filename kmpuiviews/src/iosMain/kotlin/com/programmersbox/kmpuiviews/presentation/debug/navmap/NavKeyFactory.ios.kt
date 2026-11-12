package com.programmersbox.kmpuiviews.presentation.debug.navmap

import androidx.navigation3.runtime.NavKey

// No reflection on iOS; the Navigation map disables "Open" there.
actual fun createNavKey(fqcn: String, args: List<NavArg>): NavKey? = null
