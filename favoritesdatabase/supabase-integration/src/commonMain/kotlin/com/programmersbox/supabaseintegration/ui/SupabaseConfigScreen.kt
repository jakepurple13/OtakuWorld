package com.programmersbox.supabaseintegration.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
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
    }
}
