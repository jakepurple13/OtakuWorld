package com.programmersbox.kmpuiviews.presentation.settings.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vinceglb.filekit.PlatformFile

/** True where a model file can be dropped onto the AI page (desktop). */
expect val supportsModelFileDrop: Boolean

/** Accepts a dropped file. [onHover] is true while a file is over the element. No-op without drop support. */
@Composable
expect fun Modifier.modelFileDrop(onHover: (Boolean) -> Unit, onFile: (PlatformFile) -> Unit): Modifier
