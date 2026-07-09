package com.programmersbox.kmpuiviews.presentation.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.DictionaryEntry
import com.programmersbox.favoritesdatabase.DictionarySort
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryListScreen(
    onBackPress: () -> Unit = {},
    onEntryClick: (Long) -> Unit = {},
    onAddClick: () -> Unit = {},
    vm: DictionaryListViewModel = koinViewModel(),
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var entryPendingDelete by remember { mutableStateOf<DictionaryEntry?>(null) }

    entryPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Delete \"${pending.term}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(pending)
                    entryPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dictionary") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Term (A-Z)") },
                            onClick = {
                                vm.updateSort(DictionarySort.Term)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Date Added") },
                            onClick = {
                                vm.updateSort(DictionarySort.DateAdded)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Category") },
                            onClick = {
                                vm.updateSort(DictionarySort.Category)
                                showSortMenu = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "New Entry")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    vm.updateQuery(q)
                },
                placeholder = { Text("Search term, definition, or category…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.term) },
                        supportingContent = entry.definition?.let {
                            { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        },
                        leadingContent = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { entryPendingDelete = entry }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete ${entry.term}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEntryClick(entry.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
