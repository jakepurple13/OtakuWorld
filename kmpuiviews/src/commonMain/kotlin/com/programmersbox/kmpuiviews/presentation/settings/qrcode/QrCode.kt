package com.programmersbox.kmpuiviews.presentation.settings.qrcode

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.navigateToDetails
import com.programmersbox.kmpuiviews.repository.QrCodeRepository
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.dispatchIo
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions
import qrgenerator.qrkitpainter.rememberQrKitPainter

@Serializable
data class QrCodeInfo(
    val title: String,
    val url: String,
    val imageUrl: String,
    val apiService: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareViaQrCode(
    title: String,
    url: String,
    imageUrl: String,
    apiService: String,
    onClose: () -> Unit,
) {
    ShareViaQrCode(
        qrCodeInfo = QrCodeInfo(
            title = title,
            url = url,
            imageUrl = imageUrl,
            apiService = apiService,
        ),
        onClose = onClose
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareViaQrCode(
    qrCodeInfo: QrCodeInfo,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
        onClose()
    }

    val qrCodeRepository = koinInject<QrCodeRepository>()
    val painter = rememberQrKitPainter(remember { Json.encodeToString(qrCodeInfo) })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(qrCodeInfo.title) },
                    windowInsets = WindowInsets(0.dp),
                )
            },
        ) { padding ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val graphicsLayer = rememberGraphicsLayer()
                    Box(
                        modifier = Modifier.drawWithContent {
                            // call record to capture the content in the graphics layer
                            graphicsLayer.record {
                                // draw the contents of the composable into the graphics layer
                                this@drawWithContent.drawContent()
                            }
                            // draw the graphics layer on the visible canvas
                            drawLayer(graphicsLayer)
                        }
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = "QR code",
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.medium)
                                .padding(16.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                qrCodeRepository.shareImage(
                                    bitmap = graphicsLayer.toImageBitmap(),
                                    title = qrCodeInfo.title
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(.75f)
                    ) { Text("Share") }
                }
            }
        }
    }
}

fun Painter.toImageBitmap(
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection,
): ImageBitmap {
    val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
    val canvas = Canvas(bitmap)
    CanvasDrawScope().draw(density, layoutDirection, canvas, size) {
        draw(size)
    }
    return bitmap
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrCode(
    viewModel: QrCodeScannerViewModel = koinViewModel(),
) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
            .invokeOnCompletion { navController.popBackStack() }
    }

    val qrCodeInfo = viewModel.qrCodeInfo
    val dao: ItemDao = koinInject()
    val info = LocalSourcesRepository.current

    var showLoadingDialog by remember { mutableStateOf(false) }

    LoadingDialog(
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
        ) { padding ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
            ) {
                ScannerWithPermissions(
                    onScanned = { scan ->
                        runCatching { Json.decodeFromString<QrCodeInfo>(scan) }
                            .onSuccess {
                                viewModel.qrCodeInfo = it
                                scope.launch { sheetState.expand() }
                            }
                            .onFailure { it.printStackTrace() }

                        false
                    },
                    types = listOf(CodeType.QR),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(250.dp)
                        .clip(MaterialTheme.shapes.medium)
                )

                val filePicker = rememberFilePickerLauncher(
                    type = FileKitType.Image
                ) { file ->
                    scope.launch {
                        runCatching { file?.toImageBitmap()!! }
                            .onSuccess {
                                viewModel.scanQrCodeFromImage(it)
                                scope.launch { sheetState.expand() }
                            }
                            .onFailure { it.printStackTrace() }
                    }
                }

                FilledTonalButton(
                    onClick = { filePicker.launch() },
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Upload Image") }

                Crossfade(qrCodeInfo) { target ->
                    ListItem(
                        headlineContent = { Text(target?.title ?: "Waiting for QR code") },
                        overlineContent = { Text(target?.apiService ?: "") },
                        leadingContent = {
                            ImageLoaderChoice(
                                imageUrl = target?.imageUrl ?: "",
                                name = target?.title ?: "Waiting for QR code",
                                placeHolder = { painterLogo() },
                                modifier = Modifier
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }
                    )
                }

                val source = qrCodeInfo
                    ?.apiService
                    ?.let { info.toSourceByApiServiceName(it) }

                if (source == null && qrCodeInfo != null) {
                    Text("Source not found. Please install the source.")

                    ElevatedButton(
                        onClick = {
                            scope.launch {
                                qrCodeInfo.let {
                                    dao.insertNotification(
                                        NotificationItem(
                                            id = it.toString().hashCode(),
                                            url = it.url,
                                            summaryText = "Waiting for source",
                                            notiTitle = it.title,
                                            imageUrl = it.imageUrl,
                                            source = it.apiService,
                                            contentTitle = it.title
                                        )
                                    )
                                }
                            }.invokeOnCompletion { onDismiss() }
                        },
                        modifier = Modifier.fillMaxWidth(.75f)
                    ) { Text("Save for later") }
                }

                Button(
                    onClick = {
                        scope.launch {
                            qrCodeInfo?.let {
                                info.toSourceByApiServiceName(it.apiService)
                                    ?.apiService
                                    ?.getSourceByUrlFlow(it.url)
                                    ?.dispatchIo()
                                    ?.onStart { showLoadingDialog = true }
                                    ?.catch {
                                        showLoadingDialog = false
                                    }
                                    ?.onEach { m ->
                                        showLoadingDialog = false
                                        navController.navigateToDetails(m)
                                    }
                                    ?.collect()
                            }
                        }
                    },
                    enabled = qrCodeInfo != null && source != null,
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Open") }
            }
        }
    }
}