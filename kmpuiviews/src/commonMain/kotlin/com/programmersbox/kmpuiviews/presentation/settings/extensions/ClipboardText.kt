package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.ui.platform.Clipboard

/** Plain text on the clipboard, or null. */
internal expect suspend fun Clipboard.plainText(): String?
