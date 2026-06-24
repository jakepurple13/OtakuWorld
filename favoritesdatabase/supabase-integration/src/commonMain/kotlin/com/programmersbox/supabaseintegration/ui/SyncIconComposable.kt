package com.programmersbox.supabaseintegration.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.sync.SyncConnectedStatus
import com.programmersbox.supabaseintegration.sync.SyncManager
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

private val Emerald = Color(0xFF2ecc71)
private val Alizarin = Color(0xFFe74c3c)

@Composable
fun SyncIconComposable(
    modifier: Modifier = Modifier,
    syncManager: SyncManager = koinInject(),
) {
    val state by syncManager
        .syncConnectedStatus
        .collectAsStateWithLifecycle()

    val config by syncManager
        .config
        .collectAsStateWithLifecycle()

    var showInformation by remember { mutableStateOf(false) }

    if (showInformation) {
        AlertDialog(
            onDismissRequest = { showInformation = false },
            title = { Text("Sync Icon Legend") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card {
                        ListItem(
                            headlineContent = { Text("Realtime Connection") },
                            supportingContent = { Text("Connected to wifi and will update in realtime as changes are made.") },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Emerald)
                                        .size(24.dp)
                                )
                            },
                        )
                    }

                    Card {
                        ListItem(
                            headlineContent = { Text("Polling Connection") },
                            supportingContent = { Text("Connected to cellular and will update every ${config.pollIntervalMs.milliseconds.inWholeMinutes} minutes.") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Emerald
                                )
                            }
                        )
                    }

                    Card {
                        ListItem(
                            headlineContent = { Text("Offline") },
                            supportingContent = { Text("Not connected to the internet. No syncing will occur.") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Alizarin
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInformation = false },
                ) { Text("Dismiss") }
            }
        )
    }

    IconButton(
        onClick = { showInformation = !showInformation },
        enabled = state != SyncConnectedStatus.Idle,
        modifier = modifier
    ) {
        Crossfade(state) { target ->
            when (target) {
                SyncConnectedStatus.Idle -> {}

                SyncConnectedStatus.Offline -> Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Alizarin
                )

                SyncConnectedStatus.Polling -> Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = Emerald
                )

                SyncConnectedStatus.Realtime -> Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Emerald)
                        .size(24.dp)
                )
            }
        }
    }
}