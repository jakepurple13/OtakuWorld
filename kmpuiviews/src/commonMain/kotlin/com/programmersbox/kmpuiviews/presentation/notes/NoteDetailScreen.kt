package com.programmersbox.kmpuiviews.presentation.notes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuContainerElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.SharedItemKey
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.customSharedElement

/** Shared-element key that links a Notes list card to its detail screen. */
fun noteSharedKey(itemUrl: String) = OtakuContainerElement(SharedItemKey(url = itemUrl, source = "note"))

/**
 * Full-screen note editor opened from the Notes list. The text is saved whenever the screen
 * leaves, so toolbar back, system back and predictive back all keep the edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    onBackPress: () -> Unit,
    vm: NoteDetailViewModel,
) {
    val note by vm.note.collectAsStateWithLifecycle()
    // Null until the note first loads, so an empty field never overwrites the stored text
    var text by rememberSaveable { mutableStateOf<String?>(null) }
    var deleted by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(note) {
        val current = note
        if (current != null && text == null) text = current.content
        // Deleted somewhere else while open: there's nothing left to edit
        if (current == null && text != null && !deleted) onBackPress()
    }

    DisposableEffect(Unit) {
        // Reads the state objects directly so an edit made in the leaving frame is still saved
        onDispose {
            val content = text
            if (!deleted && content != null) vm.save(content)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleted = true
                        vm.delete()
                        onBackPress()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            // Keyed by url so the morph starts before the note has loaded
            .customSharedElement(noteSharedKey(vm.itemUrl))
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(note?.itemTitle.orEmpty()) },
                    navigationIcon = {
                        IconButton(onClick = onBackPress) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = note != null,
                        ) { Icon(Icons.Default.Delete, contentDescription = "Delete note") }
                    }
                )
            },
        ) { padding ->
            OutlinedTextField(
                value = text.orEmpty(),
                onValueChange = { text = it },
                enabled = text != null,
                placeholder = { Text("Write a note…") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            )
        }
    }
}
