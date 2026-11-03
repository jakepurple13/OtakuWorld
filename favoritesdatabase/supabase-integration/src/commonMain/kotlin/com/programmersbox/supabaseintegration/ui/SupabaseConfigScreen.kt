package com.programmersbox.supabaseintegration.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.sharedcomponents.qrcode.ScanQrCode
import com.programmersbox.sharedcomponents.qrcode.ShareViaQrCode
import com.programmersbox.sharedcomponents.qrcode.qrDockTransform
import com.programmersbox.supabaseintegration.Res
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import com.programmersbox.supabaseintegration.supabase_logo_icon
import com.programmersbox.supabaseintegration.sync.SyncConfig
import com.programmersbox.supabaseintegration.sync.SyncConfigRepository
import com.programmersbox.supabaseintegration.ui.viewmodel.SupabaseConfigViewModel
import io.github.alexzhirkevich.qrose.options.QrLogoPadding
import io.github.alexzhirkevich.qrose.options.QrLogoShape
import io.github.alexzhirkevich.qrose.options.circle
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    if (showShareQrCode) {
        ShareViaQrCode(
            baseUrl = projectUrl,
            key = anonKey,
            onClose = { showShareQrCode = false }
        )
    }

    if (scanShareQrCode) {
        ScanQrCode(
            onSaveCredentials = { credentials ->
                runCatching {
                    viewModel.onProjectUrlChange(credentials.projectUrl)
                    viewModel.onAnonKeyChange(credentials.anonKey)
                }
            },
            onSaveSyncConfig = { syncConfig ->
                runCatching {
                    viewModel.onPollIntervalChange(syncConfig.pollIntervalMs.toString())
                    viewModel.onMaxRetriesChange(syncConfig.maxRetries.toString())
                    viewModel.onInitialBackoffChange(syncConfig.initialBackoffMs.toString())
                    viewModel.onMaxBackoffChange(syncConfig.maxBackoffMs.toString())
                }
            },
            onClose = { scanShareQrCode = false },
            testScannedCredentials = viewModel::testConnectionWithScannedCredentials,
            connectionResult = viewModel
                .scannedConnectionResult
                .collectAsStateWithLifecycle()
                .value
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configuration") },
                actions = {
                    IconButton(onClick = { showShareQrCode = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share via QR")
                    }
                    IconButton(onClick = { scanShareQrCode = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                    }
                },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- CREDENTIALS CARD ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Database Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = projectUrl,
                        onValueChange = viewModel::onProjectUrlChange,
                        label = { Text("Project URL") },
                        placeholder = { Text("https://xxxxxxxxxxxx.supabase.co") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = anonKey,
                        onValueChange = viewModel::onAnonKeyChange,
                        label = { Text("Anon Key") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 3, // Allowed to expand slightly for long JWTs
                    )

                    Spacer(Modifier.height(16.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = viewModel::testConnection,
                            enabled = projectUrl.isNotBlank() && anonKey.isNotBlank()
                        ) {
                            Text("Test Connection")
                        }

                        Button(
                            onClick = { viewModel.save(); onSaved() },
                            enabled = projectUrl.isNotBlank() && anonKey.isNotBlank()
                        ) {
                            Text("Save")
                        }

                        if (hasCredentials) {
                            OutlinedButton(onClick = viewModel::clear) {
                                Text("Clear")
                            }
                        }
                    }

                    // Smooth appearance for connection result
                    AnimatedVisibility(
                        visible = connectionResult != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        connectionResult?.let { result ->
                            val isSuccess = result.startsWith("✓")
                            Surface(
                                color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Text(
                                    text = result,
                                    color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // --- SYNC SETTINGS CARD ---
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Background Sync",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Controls how frequently the app syncs when offline and how it retries on failure.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pollIntervalMinutes,
                        onValueChange = viewModel::onPollIntervalChange,
                        label = { Text("Poll Interval") },
                        suffix = { Text("min") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        supportingText = { Text("How often to check for changes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = maxRetries,
                        onValueChange = viewModel::onMaxRetriesChange,
                        label = { Text("Max Retries") },
                        suffix = { Text("attempts") },
                        leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null) },
                        supportingText = { Text("Attempts before marking sync as failed") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = initialBackoffSeconds,
                        onValueChange = viewModel::onInitialBackoffChange,
                        label = { Text("Initial Retry Delay") },
                        suffix = { Text("sec") },
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        supportingText = { Text("Wait time before the first retry") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = maxBackoffSeconds,
                        onValueChange = viewModel::onMaxBackoffChange,
                        label = { Text("Max Retry Delay") },
                        suffix = { Text("sec") },
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        supportingText = { Text("Cap on exponential backoff delay") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = viewModel::saveSyncConfig) {
                            Text("Apply Settings")
                        }

                        AnimatedVisibility(visible = syncConfigSaved) {
                            Text(
                                text = "✓ Saved",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Extra bottom spacer so FABs or nav bars don't clip the bottom card
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Serializable
data class QrCodeInfo(
    val credentials: SupabaseCredentials,
    val syncConfig: SyncConfig,
    override val title: String = "Supabase Sync Config",
    override val url: String = credentials.projectUrl,
) : com.programmersbox.sharedcomponents.qrcode.QrCodeInfo

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

    val supabaseLogo = painterResource(Res.drawable.supabase_logo_icon)

    ShareViaQrCode(
        qrCodeInfo = QrCodeInfo(
            credentials = SupabaseCredentials(baseUrl, key),
            syncConfig = syncConfigInfo
        ),
        onClose = onClose,
        includeShareUrl = false,
        includeSaveImage = false,
        painterCustomize = {
            logo {
                painter = supabaseLogo
                padding = QrLogoPadding.Natural(.1f)
                shape = QrLogoShape.circle()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanQrCode(
    onSaveCredentials: (SupabaseCredentials) -> Unit,
    onSaveSyncConfig: (SyncConfig) -> Unit,
    testScannedCredentials: (String, String) -> Unit = { _, _ -> },
    connectionResult: String?,
    onClose: () -> Unit,
) {
    ScanQrCode<QrCodeInfo>(
        onRemove = onClose,
        hint = "Scan the code from Configuration → Share",
        invalidCodeText = "Not a Supabase config QR code",
        dockContent = { qrCodeInfo, scanAgain ->
            // Reset per scanned code so a result from an earlier code never shows for a new one.
            var tested by remember(qrCodeInfo) { mutableStateOf(false) }
            var credentialsSaved by remember(qrCodeInfo) { mutableStateOf(false) }
            var syncSaved by remember(qrCodeInfo) { mutableStateOf(false) }

            AnimatedContent(
                targetState = qrCodeInfo,
                transitionSpec = { qrDockTransform() },
            ) { target ->
                if (target == null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Nothing scanned yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Project and sync settings show up here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    ScannedSupabaseInfo(target)
                }
            }

            if (qrCodeInfo != null) {
                val status = when {
                    credentialsSaved && syncSaved -> "✓ Credentials and sync settings saved"
                    credentialsSaved -> "✓ Credentials saved to Configuration"
                    syncSaved -> "✓ Sync settings saved to Configuration"
                    tested && connectionResult != null -> connectionResult
                    tested -> "Testing connection…"
                    else -> "Connection not tested"
                }
                AnimatedContent(
                    targetState = status,
                    transitionSpec = { qrDockTransform() },
                ) { text ->
                    val isSuccess = text.startsWith("✓")
                    val isFailure = text.startsWith("✗")
                    Surface(
                        color = when {
                            isSuccess -> MaterialTheme.colorScheme.primaryContainer
                            isFailure -> MaterialTheme.colorScheme.errorContainer
                            else -> Color.Transparent
                        },
                        contentColor = when {
                            isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
                            isFailure -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(
                                horizontal = if (isSuccess || isFailure) 10.dp else 0.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !credentialsSaved && !syncSaved,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    OutlinedButton(
                        onClick = {
                            tested = true
                            testScannedCredentials(qrCodeInfo.credentials.projectUrl, qrCodeInfo.credentials.anonKey)
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Test connection") }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        qrCodeInfo?.let { onSaveCredentials(it.credentials) }
                        credentialsSaved = true
                    },
                    enabled = qrCodeInfo != null && !credentialsSaved,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f)
                ) { SaveButtonLabel(saved = credentialsSaved, text = "Save credentials") }

                FilledTonalButton(
                    onClick = {
                        qrCodeInfo?.let { onSaveSyncConfig(it.syncConfig) }
                        syncSaved = true
                    },
                    enabled = qrCodeInfo != null && !syncSaved,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f)
                ) { SaveButtonLabel(saved = syncSaved, text = "Save sync settings") }
            }

            AnimatedVisibility(
                visible = credentialsSaved || syncSaved,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = scanAgain,
                        modifier = Modifier.weight(1f)
                    ) { Text("Scan again") }
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) { Text("Done") }
                }
            }
        }
    )
}

@Composable
private fun SaveButtonLabel(saved: Boolean, text: String) {
    AnimatedContent(
        targetState = saved,
        transitionSpec = { qrDockTransform() },
    ) { isSaved ->
        if (isSaved) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Saved", maxLines = 1)
            }
        } else {
            Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ScannedSupabaseInfo(info: QrCodeInfo) {
    var showKey by remember(info) { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.supabase_logo_icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Text("Supabase project", style = MaterialTheme.typography.titleSmall)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "URL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp)
            )
            Text(
                info.credentials.projectUrl,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Anon key",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(64.dp)
            )
            Text(
                if (showKey) info.credentials.anonKey else maskKey(info.credentials.anonKey),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = if (showKey) 3 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            IconButton(
                onClick = { showKey = !showKey },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showKey) "Hide key" else "Show key",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SyncValueTile(formatDuration(info.syncConfig.pollIntervalMs), "Poll", Modifier.weight(1f))
            SyncValueTile(info.syncConfig.maxRetries.toString(), "Retries", Modifier.weight(1f))
            SyncValueTile(formatDuration(info.syncConfig.initialBackoffMs), "Backoff", Modifier.weight(1f))
            SyncValueTile(formatDuration(info.syncConfig.maxBackoffMs), "Max backoff", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SyncValueTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun maskKey(key: String): String =
    if (key.length <= 16) "•".repeat(key.length)
    else "${key.take(10)}…••••${key.takeLast(3)}"

internal fun formatDuration(ms: Long): String = when {
    ms >= 60_000 && ms % 60_000 == 0L -> "${ms / 60_000} min"
    ms >= 1_000 && ms % 1_000 == 0L -> "${ms / 1_000} s"
    else -> "$ms ms"
}
