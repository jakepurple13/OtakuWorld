package com.programmersbox.supabaseintegration.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import com.programmersbox.supabaseintegration.sync.SyncConfig
import com.programmersbox.supabaseintegration.sync.SyncConfigRepository
import com.programmersbox.supabaseintegration.ui.viewmodel.SupabaseConfigViewModel
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@Composable
fun SupabaseConfigScreen(
    viewModel: SupabaseConfigViewModel = koinViewModel(),
    onSaved: () -> Unit = {},
) {
    val projectUrl by viewModel.projectUrl.collectAsStateWithLifecycle()
    val anonKey by viewModel.anonKey.collectAsStateWithLifecycle()
    val connectionResult by viewModel.connectionResult.collectAsStateWithLifecycle()
    val hasCredentials by viewModel.hasCredentials.collectAsStateWithLifecycle()
    val pollIntervalMinutes by viewModel.pollIntervalMinutes.collectAsStateWithLifecycle()
    val maxRetries by viewModel.maxRetries.collectAsStateWithLifecycle()
    val initialBackoffSeconds by viewModel.initialBackoffSeconds.collectAsStateWithLifecycle()
    val maxBackoffSeconds by viewModel.maxBackoffSeconds.collectAsStateWithLifecycle()
    val syncConfigSaved by viewModel.syncConfigSaved.collectAsStateWithLifecycle()

    var showShareQrCode by remember { mutableStateOf(false) }
    var scanShareQrCode by remember { mutableStateOf(false) }

    if (showShareQrCode)
        ShareViaQrCode(
            baseUrl = projectUrl,
            key = anonKey,
            onClose = { showShareQrCode = false }
        )

    if (scanShareQrCode) {
        ScanQrCode(
            supabaseConfigViewModel = viewModel,
            onClose = { scanShareQrCode = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Supabase Configuration") },
                actions = {
                    IconButton(
                        onClick = { showShareQrCode = true }
                    ) { Icon(Icons.Default.Share, null) }
                    IconButton(
                        onClick = { scanShareQrCode = true }
                    ) { Icon(Icons.Default.QrCodeScanner, null) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(padding)
        ) {
            Text("Supabase Configuration", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = projectUrl, onValueChange = viewModel::onProjectUrlChange,
                label = { Text("Project URL") },
                placeholder = { Text("https://xxxxxxxxxxxx.supabase.co") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = anonKey, onValueChange = viewModel::onAnonKeyChange,
                label = { Text("Anon Key") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::testConnection,
                    enabled = projectUrl.isNotBlank() && anonKey.isNotBlank()
                ) { Text("Test Connection") }
                Button(
                    onClick = { viewModel.save(); onSaved() },
                    enabled = projectUrl.isNotBlank() && anonKey.isNotBlank()
                ) { Text("Save") }
                if (hasCredentials) {
                    OutlinedButton(onClick = viewModel::clear) { Text("Clear") }
                }
            }
            connectionResult?.let { result ->
                Spacer(Modifier.height(12.dp))
                Text(
                    result,
                    color = if (result.startsWith("✓")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Sync Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Controls how frequently the app syncs when offline and how it retries on failure.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = pollIntervalMinutes,
                onValueChange = viewModel::onPollIntervalChange,
                label = { Text("Poll Interval") },
                suffix = { Text("min") },
                supportingText = { Text("How often to check for changes when offline") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = maxRetries,
                onValueChange = viewModel::onMaxRetriesChange,
                label = { Text("Max Retries") },
                suffix = { Text("attempts") },
                supportingText = { Text("Number of retry attempts before marking sync as failed") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = initialBackoffSeconds,
                onValueChange = viewModel::onInitialBackoffChange,
                label = { Text("Initial Retry Delay") },
                suffix = { Text("sec") },
                supportingText = { Text("Wait time before the first retry") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = maxBackoffSeconds,
                onValueChange = viewModel::onMaxBackoffChange,
                label = { Text("Max Retry Delay") },
                suffix = { Text("sec") },
                supportingText = { Text("Cap on exponential backoff delay between retries") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::saveSyncConfig
                ) { Text("Apply") }
                if (syncConfigSaved) {
                    Text(
                        "✓ Saved",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

@Serializable
data class QrCodeInfo(
    val credentials: SupabaseCredentials,
    val syncConfig: SyncConfig,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareViaQrCode(
    baseUrl: String,
    key: String,
    onClose: () -> Unit,
) {
    val repository = koinInject<SyncConfigRepository>()
    val syncConfigInfo by repository
        .listenForChanges()
        .collectAsStateWithLifecycle(SyncConfig())

    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(SheetValue.Expanded)
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
        onClose()
    }

    val painter = rememberQrCodePainter(
        remember {
            Json.encodeToString(
                QrCodeInfo(
                    credentials = SupabaseCredentials(baseUrl, key),
                    syncConfig = syncConfigInfo
                )
            )
        }
    )

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
                            "Supabase Configuration",
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

                /*FilledTonalButton(
                    onClick = {
                        scope.launch {
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
                ) { Text("Save") }*/
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanQrCode(
    supabaseConfigViewModel: SupabaseConfigViewModel,
    onClose: () -> Unit,
    viewModel: SupabaseQrCodeScannerViewModel = koinViewModel(),
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)
    val onDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }
            .invokeOnCompletion { onClose() }
    }

    val qrCodeInfo = viewModel.qrCodeInfo

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
                    enableTorch = false,
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

                qrCodeInfo?.let { info ->
                    Text("Project url: ${info.credentials.projectUrl}")
                    Text("Project key: ${info.credentials.anonKey}")
                    Text("Poll interval: ${info.syncConfig.pollIntervalMs}")
                    Text("Max retries: ${info.syncConfig.maxRetries}")
                    Text("Initial backoff: ${info.syncConfig.initialBackoffMs}")
                    Text("Max backoff: ${info.syncConfig.maxBackoffMs}")
                }

                /*val filePicker = rememberFilePickerLauncher(
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
                ) { Text("Upload Image") }*/

                Button(
                    onClick = {
                        scope.launch {
                            qrCodeInfo?.let {
                                runCatching {
                                    supabaseConfigViewModel.onProjectUrlChange(it.credentials.projectUrl)
                                    supabaseConfigViewModel.onAnonKeyChange(it.credentials.anonKey)
                                    supabaseConfigViewModel.onPollIntervalChange(it.syncConfig.pollIntervalMs.toString())
                                    supabaseConfigViewModel.onMaxRetriesChange(it.syncConfig.maxRetries.toString())
                                    supabaseConfigViewModel.onInitialBackoffChange(it.syncConfig.initialBackoffMs.toString())
                                    supabaseConfigViewModel.onMaxBackoffChange(it.syncConfig.maxBackoffMs.toString())
                                }
                            }
                        }
                    },
                    enabled = qrCodeInfo != null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Save") }
            }
        }
    }
}

class SupabaseQrCodeScannerViewModel : ViewModel() {
    var qrCodeInfo by mutableStateOf<QrCodeInfo?>(null)

    fun scanQrCodeFromImage(bitmap: ImageBitmap) {

    }
}
