package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier,
) {
    ScannerWithPermissions(
        onScanned = { scan ->
            onScan(scan)
            false
        },
        types = listOf(CodeType.QR),
        cameraPosition = CameraPosition.BACK,
        enableTorch = torchState,
        permissionDeniedContent = { permissionState ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .sizeIn(maxWidth = 250.dp, maxHeight = 250.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.shapes.medium
                    )
            ) {
                Text(
                    text = "Camera permission required",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(6.dp)
                )
                ElevatedButton(
                    onClick = dropUnlessResumed { permissionState.goToSettings() }
                ) { Text("Open Settings") }
            }
        },
        modifier = modifier
    )
}