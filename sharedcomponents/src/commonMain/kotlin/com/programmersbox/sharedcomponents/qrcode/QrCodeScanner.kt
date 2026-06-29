package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
inline fun <reified T> ScanQrCode(
    noinline onOpen: (T) -> Unit,
    crossinline onRemove: () -> Unit,
    crossinline customUi: @Composable (T?) -> Unit,
    viewModel: QrCodeViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded)
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
            .invokeOnCompletion { onRemove() }
    }

    var qrCodeInfo by remember { mutableStateOf<T?>(null) }

    var showLoadingDialog by remember { mutableStateOf(false) }

    QrCodeLoadingDialog(
        showLoadingDialog = showLoadingDialog,
        onDismissRequest = { showLoadingDialog = false }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Scan QR code") },
                    windowInsets = WindowInsets(0.dp),
                )
            },
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
        ) { padding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
            ) {
                var torchState by remember { mutableStateOf(false) }

                CameraView(
                    onScan = { scan ->
                        runCatching { Json.decodeFromString<T>(scan) }
                            .onSuccess {
                                qrCodeInfo = it
                                scope.launch { sheetState.expand() }
                            }
                            .onFailure { it.printStackTrace() }
                    },
                    torchState = torchState,
                    modifier = Modifier
                        .sizeIn(maxWidth = 250.dp, maxHeight = 250.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .animateContentSize()
                )

                val filePicker = rememberFilePickerLauncher(
                    type = FileKitType.Image
                ) { file ->
                    scope.launch {
                        runCatching { file?.toImageBitmap()!! }
                            .onSuccess {
                                scope.launch {
                                    viewModel.scanQrCodeFromImage<T>(it)
                                        .onSuccess {
                                            sheetState.expand()
                                            qrCodeInfo = it
                                        }
                                }
                            }
                            .onFailure { it.printStackTrace() }
                    }
                }

                FilledTonalButton(
                    onClick = { filePicker.launch() },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Upload Image") }

                customUi(qrCodeInfo)

                Button(
                    onClick = { qrCodeInfo?.let { onOpen(it) } },
                    enabled = qrCodeInfo != null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Open") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QrCodeLoadingDialog(
    showLoadingDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (showLoadingDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.0.dp))
            ) {
                Column {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = "Loading...", Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
expect fun CameraView(
    onScan: (String) -> Unit,
    torchState: Boolean,
    modifier: Modifier = Modifier,
)