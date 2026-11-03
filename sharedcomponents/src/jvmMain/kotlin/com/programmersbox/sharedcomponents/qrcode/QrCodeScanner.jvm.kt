package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.enums.TorchMode
import com.kashif.cameraK.state.CameraKEvent
import com.kashif.qrscannerplugin.rememberQRScannerPlugin
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import java.io.File
import java.net.URI

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun CameraView(
    onScan: (String) -> Unit,
    torchState: Boolean,
    unavailableContent: @Composable () -> Unit,
    overlay: @Composable BoxScope.(hasTorch: Boolean) -> Unit,
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
            },
            errorContent = { unavailableContent() },
            modifier = Modifier.fillMaxSize(),
        ) { state ->
            LaunchedEffect(torchState) {
                state.controller.setTorchMode(if (torchState) TorchMode.ON else TorchMode.OFF)
            }
            // Desktop webcams have no torch.
            Box(Modifier.fillMaxSize()) { overlay(false) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
actual fun Modifier.qrImageDropTarget(
    onImage: (ImageBitmap) -> Unit,
    onHoverChange: (Boolean) -> Unit,
): Modifier = composed {
    val target = remember(onImage, onHoverChange) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = onHoverChange(true)
            override fun onExited(event: DragAndDropEvent) = onHoverChange(false)
            override fun onEnded(event: DragAndDropEvent) = onHoverChange(false)

            override fun onDrop(event: DragAndDropEvent): Boolean {
                onHoverChange(false)
                val files = event.dragData() as? DragData.FilesList ?: return false
                val bitmap = files.readFiles()
                    .firstNotNullOfOrNull { uri ->
                        runCatching {
                            Image.makeFromEncoded(File(URI(uri)).readBytes()).toComposeImageBitmap()
                        }.getOrNull()
                    } ?: return false
                onImage(bitmap)
                return true
            }
        }
    }
    dragAndDropTarget(
        shouldStartDragAndDrop = { it.dragData() is DragData.FilesList },
        target = target,
    )
}
