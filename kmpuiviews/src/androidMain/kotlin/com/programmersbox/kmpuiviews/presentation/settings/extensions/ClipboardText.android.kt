package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.ui.platform.Clipboard

internal actual suspend fun Clipboard.plainText(): String? =
    getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
