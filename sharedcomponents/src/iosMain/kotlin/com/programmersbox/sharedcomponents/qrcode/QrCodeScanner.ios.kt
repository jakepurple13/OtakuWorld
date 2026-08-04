package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(markerClass = [ExperimentalMaterial3Api::class])
@Composable
actual fun CameraView(onScan: (String) -> Unit, torchState: Boolean, modifier: Modifier) {
}