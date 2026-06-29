package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.state.CameraKEvent
import com.kashif.qrscannerplugin.rememberQRScannerPlugin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun CameraView(
    onScan: (String) -> Unit,
    torchState: Boolean,
    modifier: Modifier,
) {
    val qrScannerPlugin = rememberQRScannerPlugin()
    val cameraState by rememberCameraKState(
        setupPlugins = { stateHolder ->
            stateHolder.attachPlugin(qrScannerPlugin)
            stateHolder.pluginScope.launch {
                stateHolder.events.collect { event ->
                    when (event) {
                        is CameraKEvent.QRCodeScanned -> {
                            onScan(event.qrCode)
                        }

                        else -> {}
                    }
                }
            }
        }
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        CameraKScreen(
            cameraState = cameraState,
            loadingContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularWavyProgressIndicator()
                        Text(
                            text = "Initializing Camera...",
                            color = Color.White,
                        )
                    }
                }
            }
        ) { state ->
            LaunchedEffect(torchState) {
                state.controller.setTorchMode(if (torchState) TorchMode.ON else TorchMode.OFF)
            }
        }
    }
}