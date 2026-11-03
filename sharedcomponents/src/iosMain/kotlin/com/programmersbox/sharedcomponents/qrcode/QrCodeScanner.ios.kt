package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

@OptIn(markerClass = [ExperimentalMaterial3Api::class])
@Composable
actual fun CameraView(
    onScan: (String) -> Unit,
    torchState: Boolean,
    unavailableContent: @Composable () -> Unit,
    overlay: @Composable BoxScope.(hasTorch: Boolean) -> Unit,
    modifier: Modifier,
) {
    unavailableContent()
}

actual fun Modifier.qrImageDropTarget(
    onImage: (ImageBitmap) -> Unit,
    onHoverChange: (Boolean) -> Unit,
): Modifier = this
