package com.programmersbox.koogintegration.customscraper.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CustomScraperScreen(
    onBack: () -> Unit,
    viewModel: CustomScraperViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Scraper") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(padding)
        ) {
            OutlinedTextField(
                state = viewModel.currentTextFieldState,
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.extractDetails() }
                    ) { Icon(Icons.Default.AddLink, null) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            if (uiState.isLoading) {
                CircularWavyProgressIndicator()
            }

            Text(
                text = uiState.customScrapeKmpInfoModel.toString(),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
//https://mangadex.org/title/8572da1b-3b5d-44aa-a40c-8a92b512c336/dungeon-no-osananajimi