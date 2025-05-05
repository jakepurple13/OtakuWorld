package com.programmersbox.uiviews.presentation.settings.updateprerelease

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.plus
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.uiviews.presentation.components.OtakuPullToRefreshBox
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrereleaseScreen(
    viewModel: PrereleaseViewModel = koinViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prerelease Builds") },
                subtitle = {
                    when (val state = viewModel.uiState) {
                        is PrereleaseUiState.Error -> Text(state.message)
                        is PrereleaseUiState.Success -> Text(state.latestRelease.getUpdatedTime().formatTime())
                        else -> {}
                    }
                },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        OtakuPullToRefreshBox(
            isRefreshing = viewModel.uiState is PrereleaseUiState.Loading,
            onRefresh = { viewModel.reload() },
            paddingValues = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(padding + LocalNavHostPadding.current)
                    .fillMaxSize()
            ) {
                when (val state = viewModel.uiState) {
                    is PrereleaseUiState.Error -> Text(state.message)
                    is PrereleaseUiState.Success -> {
                        val release = state.latestRelease

                        Text(
                            release.name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(8.dp)
                        )

                        release.assets.forEach {
                            OutlinedCard(
                                modifier = Modifier.animateContentSize()
                            ) {
                                ListItem(
                                    overlineContent = { Text("Updated at: ${it.updatedAt.formatTime()}") },
                                    headlineContent = { Text(it.name) },
                                    trailingContent = {
                                        IconButton(
                                            onClick = { viewModel.update(it.url) }
                                        ) { Icon(Icons.Default.Download, null) }
                                    },
                                )

                                viewModel.downloadMap[it.url]?.let {
                                    HorizontalDivider()
                                    DownloadStatus(it)
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
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

fun Instant.formatTime(): String {
    return SimpleDateFormat.getDateTimeInstance().format(this.toEpochMilliseconds())
}