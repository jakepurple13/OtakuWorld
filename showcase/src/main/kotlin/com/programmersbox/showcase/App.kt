package com.programmersbox.showcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.showcase.generated.ShowcaseEntry
import com.programmersbox.showcase.generated.ShowcaseRegistry

private const val ALL_GROUP = "All"

@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            var selectedGroup by remember { mutableStateOf<String?>(null) }
            val groups = remember { ShowcaseRegistry.entries.map { it.group }.distinct().sorted() }

            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    NavigationRailItem(
                        selected = selectedGroup == ALL_GROUP,
                        onClick = { selectedGroup = ALL_GROUP },
                        icon = { Icon(Icons.Default.Apps, contentDescription = ALL_GROUP) },
                        label = { Text(ALL_GROUP) },
                    )
                    groups.forEach { group ->
                        NavigationRailItem(
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            icon = { Icon(Icons.Default.Apps, contentDescription = group) },
                            label = { Text(group) },
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (val group = selectedGroup) {
                        null -> WelcomePlaceholder()
                        else -> {
                            val entries = if (group == ALL_GROUP) {
                                ShowcaseRegistry.entries
                            } else {
                                ShowcaseRegistry.entries.filter { it.group == group }
                            }
                            ComponentList(entries)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Select a group from the rail to browse components")
    }
}

@Composable
private fun ComponentList(entries: List<ShowcaseEntry>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(entries) { entry ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium)
                    Text(entry.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    entry.content()
                }
            }
        }
    }
}
