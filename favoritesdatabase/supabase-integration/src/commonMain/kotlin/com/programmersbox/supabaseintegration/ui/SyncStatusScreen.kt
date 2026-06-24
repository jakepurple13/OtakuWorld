package com.programmersbox.supabaseintegration.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.sync.SyncState
import com.programmersbox.supabaseintegration.ui.viewmodel.SyncViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(viewModel: SyncViewModel = koinViewModel()) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sync Status") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Push content to the center
            Spacer(modifier = Modifier.weight(1f))

            // Animated Status UI
            AnimatedContent(
                targetState = syncState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "Sync State Animation"
            ) { state ->
                // 1. Map current state to visual properties
                val syncState = when (state) {
                    is SyncState.Idle -> SyncUiState(
                        icon = Icons.Outlined.CloudDone,
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Up to Date",
                        subtitle = "All your data is safely backed up to the cloud."
                    )

                    is SyncState.Syncing -> SyncUiState(
                        icon = Icons.Outlined.CloudSync,
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Syncing...",
                        subtitle = "Updating your data, please wait."
                    )

                    is SyncState.Error -> SyncUiState(
                        icon = Icons.Outlined.CloudOff,
                        tint = MaterialTheme.colorScheme.error,
                        title = "Sync Failed",
                        subtitle = state.message
                    )

                    is SyncState.Offline -> SyncUiState(
                        icon = Icons.Outlined.WifiOff,
                        tint = MaterialTheme.colorScheme.outline,
                        title = "Offline",
                        subtitle = "Waiting for network connection to resume syncing."
                    )
                }

                // 2. Build the visual representation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Circular Icon Background
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                color = syncState.tint.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = syncState.icon,
                            contentDescription = syncState.title,
                            tint = syncState.tint,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = syncState.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = syncState.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Smoothly show/hide progress indicator
            AnimatedVisibility(
                visible = syncState is SyncState.Syncing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                // Using standard LinearProgressIndicator, replace with LinearWavyProgressIndicator
                // if you are using the specific M3 experimental component
                LinearWavyProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 16.dp)
                )
            }

            // Push the button to the bottom
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = viewModel::triggerSync,
                enabled = syncState is SyncState.Idle || syncState is SyncState.Error,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Taller button for better touch targets
            ) {
                Text("Sync Now", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Stable
data class SyncUiState(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val subtitle: String,
)