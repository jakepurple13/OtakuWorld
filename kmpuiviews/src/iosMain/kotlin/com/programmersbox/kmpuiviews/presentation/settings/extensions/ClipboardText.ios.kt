package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun Clipboard.plainText(): String? = getClipEntry()?.getPlainText()
