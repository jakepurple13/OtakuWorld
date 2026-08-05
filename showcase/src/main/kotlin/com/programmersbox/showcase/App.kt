package com.programmersbox.showcase

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.programmersbox.kmpuiviews.presentation.navigation.scenestrategy.BottomSheetSceneStrategy
import com.programmersbox.showcase.annotations.ShowcaseEntry
import com.programmersbox.showcase.annotations.ShowcaseRegistryProvider
import kotlinx.serialization.Serializable
import java.util.ServiceLoader

private val allEntries: List<ShowcaseEntry> by lazy {
    ServiceLoader.load(ShowcaseRegistryProvider::class.java).flatMap { it.entries }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App(
    themeMode: Boolean,
    onThemeModeChange: (Boolean) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        val groups = remember {
            allEntries
                .groupBy { it.group }
                .toSortedMap()
        }

        val backStack = remember { mutableStateListOf<NavKey>(Home) }

        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NavigationRailItem(
                        selected = (backStack.lastOrNull() as? Sample)?.key == null,
                        onClick = { backStack.add(Sample(null)) },
                        icon = { Icon(Icons.Default.Apps, contentDescription = "All") },
                        label = { Text("All") },
                    )
                    groups.forEach { group ->
                        NavigationRailItem(
                            selected = (backStack.lastOrNull() as? Sample)?.key == group.key,
                            onClick = { backStack.add(Sample(group.key)) },
                            icon = { Icon(Icons.Default.Apps, contentDescription = group.key) },
                            label = { Text(group.key) },
                        )
                    }
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Component Showcase") },
                        actions = {
                            FilledTonalIconToggleButton(
                                checked = themeMode,
                                onCheckedChange = onThemeModeChange
                            ) { Icon(Icons.Default.DarkMode, contentDescription = "Theme") }
                        }
                    )
                }
            ) { padding ->
                NavDisplay(
                    backStack = backStack,
                    //onBack = { backStack.removeLastOrNull() },
                    sceneStrategies = listOf(
                        rememberListDetailSceneStrategy<NavKey>(),
                        DialogSceneStrategy(),
                        BottomSheetSceneStrategy()
                    ),
                    entryDecorators = listOf(
                        //sharedEntryInSceneNavEntryDecorator,
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<Home> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .padding(padding)
                            ) {
                                WelcomePlaceholder(
                                    groups = groups.keys.toList(),
                                    onGroupClick = { backStack.add(Sample(it)) }
                                )
                            }
                        }

                        entry<Sample> { sample ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .padding(padding)
                            ) {
                                val entries = if (sample.key == null) {
                                    allEntries
                                } else {
                                    allEntries.filter { it.group == sample.key }
                                }
                                ComponentList(entries)
                            }
                        }
                    },
                    transitionSpec = {
                        // Slide in from right when navigating forward
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                    },
                    popTransitionSpec = {
                        // Slide in from left when navigating back
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { it })
                    },
                    predictivePopTransitionSpec = {
                        // Slide in from left when navigating back
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { it })
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Serializable
data object Home : NavKey

@Serializable
data class Sample(val key: String?) : NavKey

@Composable
private fun WelcomePlaceholder(
    groups: List<String>,
    onGroupClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(groups) { entry ->
            Card(
                onClick = { onGroupClick(entry) },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        entry,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
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
