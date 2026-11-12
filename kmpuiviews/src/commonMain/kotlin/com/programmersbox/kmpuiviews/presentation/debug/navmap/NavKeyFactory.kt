package com.programmersbox.kmpuiviews.presentation.debug.navmap

import androidx.navigation3.runtime.NavKey

/**
 * Builds the key [fqcn] with sample arguments (String "sample", numbers 1, Boolean false, nullable null)
 * so the Navigation map can open any screen. Null when the key can't be built.
 */
expect fun createNavKey(fqcn: String, args: List<NavArg>): NavKey?
