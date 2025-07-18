package com.programmersbox.kmpuiviews.presentation.settings.qrcode

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.rememberUseLogoInQrCode
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.LoadingDialog
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.repository.QrCodeRepository
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.dispatchIo
import io.github.alexzhirkevich.qrose.options.QrLogoPadding
import io.github.alexzhirkevich.qrose.options.QrLogoShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
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
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    var includeLogo by rememberUseLogoInQrCode()
    val qrCodeRepository = koinInject<QrCodeRepository>()
    val logoPainter = painterLogo()
    val painter = rememberQrCodePainter(
        remember { Json.encodeToString(qrCodeInfo) }
    ) {
        if (includeLogo) {
            logo {
                painter = logoPainter
                padding = QrLogoPadding.Natural(.1f)
                shape = QrLogoShape.circle()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = sheetState
    ) {
        Scaffold { padding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                val graphicsLayer = rememberGraphicsLayer()
                SelectionContainer {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
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
                        Text(
                            qrCodeInfo.title,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Image(
                            painter = painter,
                            contentDescription = "QR code",
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.medium)
                                .padding(16.dp)
                                .animateContentSize()
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Include Logo")

                    Switch(
                        checked = includeLogo,
                        onCheckedChange = { includeLogo = it }
                    )
                }

                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            //TODO: In an update, change to copy to clipboard
                            qrCodeRepository.shareImage(
                                bitmap = graphicsLayer.toImageBitmap(),
                                title = qrCodeInfo.title
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Share") }

                ElevatedButton(
                    onClick = {
                        scope.launch {
                            qrCodeRepository.saveImage(
                                bitmap = graphicsLayer.toImageBitmap(),
                                title = qrCodeInfo.title
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Save") }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            qrCodeRepository.shareUrl(qrCodeInfo.url, qrCodeInfo.title)
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Share Url") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanQrCode(
    viewModel: QrCodeScannerViewModel = koinViewModel(),
) {
    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

    val navController = LocalNavActions.current
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
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
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
                    cameraPosition = CameraPosition.BACK,
                    permissionDeniedContent = { permissionState ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .sizeIn(maxWidth = 250.dp, maxHeight = 250.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface,
                                    MaterialTheme.shapes.medium
                                )
                        ) {
                            Text(
                                text = "Camera is required for QR Code scanning",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(6.dp)
                            )
                            ElevatedButton(
                                onClick = { permissionState.goToSettings() }
                            ) { Text("Open Settings") }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .sizeIn(maxWidth = 250.dp, maxHeight = 250.dp)
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
                    shapes = ButtonDefaults.shapes(),
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
                                colorFilter = colorFilter,
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
                        shapes = ButtonDefaults.shapes(),
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
                                        navController.details(m)
                                    }
                                    ?.collect()
                            }
                        }
                    },
                    enabled = qrCodeInfo != null && source != null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Open") }
            }
        }
    }
}