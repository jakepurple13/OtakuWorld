package com.programmersbox.kmpuiviews.presentation.settings.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vinceglb.filekit.PlatformFile

actual val supportsModelFileDrop: Boolean = false

@Composable
actual fun Modifier.modelFileDrop(onHover: (Boolean) -> Unit, onFile: (PlatformFile) -> Unit): Modifier = this
