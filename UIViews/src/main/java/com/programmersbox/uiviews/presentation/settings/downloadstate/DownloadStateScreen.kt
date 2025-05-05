package com.programmersbox.uiviews.presentation.settings.downloadstate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuHazeScaffold
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.uiviews.checkers.DownloadAndInstallState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadStateScreen(
    viewModel: DownloadStateViewModel = koinViewModel(),
) {
    OtakuHazeScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(viewModel.downloadList) {
                DownloadItem(
                    item = it,
                    onCancel = { viewModel.cancelWorker(it.id) },
                    onInstall = { viewModel.install(it.url) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun DownloadItem(
    item: DownloadAndInstallState,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Download") },
            text = { Text("Are you sure you want to cancel this download?") },
            confirmButton = {
                IconButton(
                    onClick = {
                        onCancel()
                        showCancelDialog = false
                    }
                ) { Text("Yes") }
            },
            dismissButton = {
                IconButton(
                    onClick = { showCancelDialog = false }
                ) { Text("No") }
            }
        )
    }

    OutlinedCard(
        onClick = {
            if (item.status is DownloadAndInstallStatus.Installing || item.status is DownloadAndInstallStatus.Error) {
                onInstall()
            }
        },
        modifier = modifier.animateContentSize()
    ) {
        ListItem(
            headlineContent = { Text(item.name) },
            trailingContent = {
                IconButton(
                    onClick = { showCancelDialog = true }
                ) { Icon(Icons.Default.Cancel, null) }
            }
        )
        AnimatedVisibility(item.status is DownloadAndInstallStatus.Installing) {
            Text(
                "Click to Install",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        HorizontalDivider()
        DownloadStatus(item.status)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadStatus(
    downloadStatus: DownloadAndInstallStatus,
) {
    when (val state = downloadStatus) {
        DownloadAndInstallStatus.Downloaded -> {
            ListItem(
                headlineContent = { Text("Downloaded") },
                supportingContent = { LinearWavyProgressIndicator() },
            )
        }

        is DownloadAndInstallStatus.Downloading -> {
            ListItem(
                headlineContent = { Text("Downloading ${state.progress * 100}%") },
                supportingContent = { LinearWavyProgressIndicator(progress = { state.progress }) },
            )
        }

        is DownloadAndInstallStatus.Error -> {
            ListItem(
                headlineContent = { Text("Error") },
                supportingContent = { Text(state.message) },
            )
        }

        DownloadAndInstallStatus.Installed -> {
            ListItem(
                headlineContent = { Text("Installed") },
                trailingContent = { Icon(Icons.Default.DownloadDone, null) },
                supportingContent = { LinearWavyProgressIndicator(progress = { 1f }) },
            )
        }

        DownloadAndInstallStatus.Installing -> {
            ListItem(
                headlineContent = { Text("Installing") },
                supportingContent = { LinearWavyProgressIndicator() },
            )
        }
    }
}