package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.asAwtTransferable
import java.awt.datatransfer.DataFlavor

@OptIn(ExperimentalComposeUiApi::class)
internal actual suspend fun Clipboard.plainText(): String? =
    getClipEntry()?.asAwtTransferable
        ?.takeIf { it.isDataFlavorSupported(DataFlavor.stringFlavor) }
        ?.let { runCatching { it.getTransferData(DataFlavor.stringFlavor) }.getOrNull() } as? String
