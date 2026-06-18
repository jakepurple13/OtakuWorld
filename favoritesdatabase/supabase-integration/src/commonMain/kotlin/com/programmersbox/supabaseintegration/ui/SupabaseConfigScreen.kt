package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.ui.viewmodel.SupabaseConfigViewModel
import org.koin.compose.viewmodel.koinViewModel

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

    Scaffold { padding ->
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
                Button(onClick = viewModel::saveSyncConfig) { Text("Apply") }
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
