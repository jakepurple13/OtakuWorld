package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryFormScreen(
    onDone: () -> Unit = {},
    vm: DictionaryFormViewModel,
) {
    val entry by vm.entry.collectAsStateWithLifecycle()
    val isEdit = entry != null

    var term by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var reading by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }

    LaunchedEffect(entry) {
        val current = entry
        if (current != null && !prefilled) {
            term = current.term
            definition = current.definition.orEmpty()
            reading = current.reading.orEmpty()
            category = current.category.orEmpty()
            notes = current.notes.orEmpty()
            language = current.language.orEmpty()
            prefilled = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Entry" else "New Entry") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        enabled = term.isNotBlank(),
                        onClick = {
                            vm.save(
                                term = term,
                                definition = definition.ifBlank { null },
                                reading = reading.ifBlank { null },
                                category = category.ifBlank { null },
                                notes = notes.ifBlank { null },
                                language = language.ifBlank { null },
                            )
                            onDone()
                        }
                    ) { Text("Save") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = term,
                onValueChange = { term = it },
                label = { Text("Term *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = reading,
                onValueChange = { reading = it },
                label = { Text("Reading") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = definition,
                onValueChange = { definition = it },
                label = { Text("Definition") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Language") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            if (isEdit) {
                Text(
                    text = "Added: ${entry?.dateAdded}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
