package com.programmersbox.supabaseintegration.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.supabaseintegration.sync.SyncConnectedStatus
import com.programmersbox.supabaseintegration.sync.SyncManager
import org.koin.compose.koinInject

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

    Box(modifier = modifier) {
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