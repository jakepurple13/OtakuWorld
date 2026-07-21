package com.programmersbox.koogintegration.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programmersbox.koogintegration.DownloadedModel
import com.programmersbox.koogintegration.ModelManager
import com.programmersbox.sharedcomponents.components.GenericBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelListScreen(
    modelManager: ModelManager = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    var models by remember { mutableStateOf<List<DownloadedModel>>(emptyList()) }

    // Load models when the screen first appears
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            models = modelManager.listModels()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloaded Models") },
                navigationIcon = { GenericBackButton() }
            )
        }
    ) { paddingValues ->

        // Apply the Scaffold's padding values here so content sits below the TopAppBar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (models.isEmpty()) {
                Text(
                    text = "No models downloaded yet.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = models,
                        key = { it.fileName }
                    ) { model ->
                        ModelListItem(
                            model = model,
                            onDeleteClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val success = modelManager.deleteModel(model.fileName)
                                    if (success) {
                                        models = modelManager.listModels()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelListItem(
    model: DownloadedModel,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Downloaded: ${formatEpoch(model.lastModifiedEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Size: ${formatBytes(model.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${model.fileName}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatEpoch(epochMillis: Long): String {
    if (epochMillis == 0L) return "Unknown"

    val date = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val year = date.year
    val month = date.month.number.toString().padStart(2, '0')
    val day = date.day.toString().padStart(2, '0')

    return "$year-$month-$day"
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> "${(gb * 10).toInt() / 10.0} GB"
        mb >= 1.0 -> "${(mb * 10).toInt() / 10.0} MB"
        kb >= 1.0 -> "${(kb * 10).toInt() / 10.0} KB"
        else -> "$bytes Bytes"
    }
}