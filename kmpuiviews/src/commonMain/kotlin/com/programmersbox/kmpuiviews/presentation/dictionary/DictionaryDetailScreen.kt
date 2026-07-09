package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.DictionaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryDetailScreen(
    onBackPress: () -> Unit = {},
    onEditClick: (Long) -> Unit = {},
    vm: DictionaryDetailViewModel,
) {
    val entry by vm.entry.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) onBackPress()
    }

    val current = entry

    if (showDeleteDialog && current != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete entry?") },
            text = { Text("Delete \"${current.term}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete()
                    showDeleteDialog = false
                    deleted = true
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.term.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (current != null) {
                        IconButton(onClick = { onEditClick(current.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (current != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                DetailField("Reading", current.reading)
                DetailField("Definition", current.definition)
                DetailField("Category", current.category)
                DetailField("Notes", current.notes)
                DetailField("Language", current.language)
                DetailField("Date Added", current.dateAdded.toString())
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
