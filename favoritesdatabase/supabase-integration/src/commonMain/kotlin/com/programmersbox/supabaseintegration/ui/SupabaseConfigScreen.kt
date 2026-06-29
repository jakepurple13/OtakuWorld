package com.programmersbox.supabaseintegration.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.sharedcomponents.qrcode.ScanQrCode
import com.programmersbox.sharedcomponents.qrcode.ShareViaQrCode
import com.programmersbox.supabaseintegration.credentials.SupabaseCredentials
import com.programmersbox.supabaseintegration.sync.SyncConfig
import com.programmersbox.supabaseintegration.sync.SyncConfigRepository
import com.programmersbox.supabaseintegration.ui.viewmodel.SupabaseConfigViewModel
import kotlinx.serialization.Serializable
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
            onClose = { scanShareQrCode = false }
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

    ShareViaQrCode(
        qrCodeInfo = QrCodeInfo(
            credentials = SupabaseCredentials(baseUrl, key),
            syncConfig = syncConfigInfo
        ),
        onClose = onClose,
        includeShareUrl = false,
        includeSaveImage = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanQrCode(
    onSaveCredentials: (SupabaseCredentials) -> Unit,
    onSaveSyncConfig: (SyncConfig) -> Unit,
    onClose: () -> Unit,
) {
    ScanQrCode<QrCodeInfo>(
        onOpen = { onSaveCredentials(it.credentials) },
        onRemove = onClose,
        customUi = { qrCodeInfo ->
            qrCodeInfo?.let { info ->
                Text("Project url: ${info.credentials.projectUrl}")
                Text("Project key: ${info.credentials.anonKey}")

                Button(
                    onClick = { onSaveCredentials(info.credentials) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Save Credentials") }

                Text("Poll interval: ${info.syncConfig.pollIntervalMs}")
                Text("Max retries: ${info.syncConfig.maxRetries}")
                Text("Initial backoff: ${info.syncConfig.initialBackoffMs}")
                Text("Max backoff: ${info.syncConfig.maxBackoffMs}")

                Button(
                    onClick = { onSaveSyncConfig(info.syncConfig) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth(.75f)
                ) { Text("Save Config") }
            }

        },
        showSaveOpenButton = false
    )
}