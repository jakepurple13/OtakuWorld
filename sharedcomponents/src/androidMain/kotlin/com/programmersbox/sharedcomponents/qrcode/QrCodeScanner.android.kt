package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@OptIn(markerClass = [ExperimentalMaterial3Api::class])
@Composable
actual fun CameraView(
    onScan: (String) -> Unit,
    torchState: Boolean,
    unavailableContent: @Composable () -> Unit,
    overlay: @Composable BoxScope.(hasTorch: Boolean) -> Unit,
    modifier: Modifier,
) {
    var permissionDenied by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        ScannerWithPermissions(
            onScanned = { scan ->
                onScan(scan)
                false
            },
            types = listOf(CodeType.QR),
            cameraPosition = CameraPosition.BACK,
            enableTorch = torchState,
            permissionDeniedContent = { permissionState ->
                SideEffect { permissionDenied = true }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Camera permission is needed to scan QR codes",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    FilledTonalButton(
                        onClick = dropUnlessResumed { permissionState.goToSettings() }
                    ) { Text("Open Settings") }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (!permissionDenied) overlay(true)
    }
}

actual fun Modifier.qrImageDropTarget(
    onImage: (ImageBitmap) -> Unit,
    onHoverChange: (Boolean) -> Unit,
): Modifier = this
