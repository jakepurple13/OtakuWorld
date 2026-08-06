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
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.BaseWindow
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.navigation.scenestrategy.BottomSheetSceneStrategy
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.presentation.settings.general.AmoledModeSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.ExpressivenessSetting
import com.programmersbox.kmpuiviews.presentation.settings.general.ThemeSetting
import com.programmersbox.showcase.annotations.ShowcaseEntry
import com.programmersbox.showcase.annotations.ShowcaseRegistryProvider
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.util.ServiceLoader

private val allEntries: List<ShowcaseEntry> by lazy {
    ServiceLoader.load(ShowcaseRegistryProvider::class.java).flatMap { it.entries }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val groups = remember {
            allEntries
                .groupBy { it.group }
                .toSortedMap()
        }

        val backStack = remember { mutableStateListOf<NavKey>(Home) }

        var showThemeSettings by remember { mutableStateOf(false) }

        if (showThemeSettings) {
            BaseWindow(
                title = "Theme Settings",
                exitApplication = { showThemeSettings = false },
            ) {
                val handling: NewSettingsHandling = koinInject()

                SettingsScaffold(
                    title = "Theme",
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var isAmoledMode by handling.rememberIsAmoledMode()
                    CategoryGroup {
                        item {
                            ThemeSetting(
                                handling = handling,
                                isAmoledMode = isAmoledMode
                            )
                        }

                        item {
                            AmoledModeSetting(
                                isAmoledMode = isAmoledMode,
                                onAmoledModeChange = { isAmoledMode = it }
                            )
                        }

                        item { ExpressivenessSetting(handling = handling) }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(contentType = "navItem") {
                        NavigationRailItem(
                            selected = backStack.lastOrNull() == Home,
                            onClick = { backStack.navigateToTop(Home) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "All") },
                            label = { Text("Home") },
                        )
                    }
                    item(contentType = "navItem") {
                        NavigationRailItem(
                            selected = backStack.lastOrNull() == All,
                            onClick = { backStack.navigateToTop(All) },
                            icon = { Icon(Icons.Default.AllInbox, contentDescription = "All") },
                            label = { Text("All") },
                        )
                    }
                    groups.forEach { group ->
                        item(contentType = "navItem") {
                            NavigationRailItem(
                                selected = (backStack.lastOrNull() as? Sample)?.key == group.key,
                                onClick = { backStack.navigateToTop(Sample(group.key)) },
                                icon = { Icon(Icons.Default.Apps, contentDescription = group.key) },
                                label = { Text(group.key) },
                            )
                        }
                    }
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Component Showcase") },
                        actions = {
                            FilledTonalIconButton(
                                onClick = { showThemeSettings = true }
                            ) { Icon(Icons.Default.DarkMode, contentDescription = "Theme") }
                        },
                        subtitle = { Text("${allEntries.size} components") }
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
                                    onGroupClick = { backStack.navigateToTop(Sample(it)) },
                                    onAllClick = { backStack.navigateToTop(All) }
                                )
                            }
                        }

                        entry<All> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .padding(padding)
                            ) { ComponentList(allEntries) }
                        }

                        entry<Sample> { sample ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .padding(padding)
                            ) {
                                val entries = remember {
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

fun <T> SnapshotStateList<T>.navigateToTop(destination: T) {
    // 1. Remove the item if it exists anywhere in the list
    this.remove(destination)

    // 2. Add it to the end of the list to make it the active screen
    this.add(destination)
}

@Serializable
data object Home : NavKey

@Serializable
data class Sample(val key: String) : NavKey

@Serializable
data object All : NavKey

@Composable
private fun WelcomePlaceholder(
    groups: List<String>,
    onGroupClick: (String) -> Unit,
    onAllClick: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(contentType = "group") {
            Card(
                onClick = onAllClick,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "All",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        items(
            items = groups,
            contentType = { _ -> "group" },
            key = { it }
        ) { entry ->
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
        item(contentType = "contentType1") {
            ListItem(
                headlineContent = { Text("${entries.size} ${entries.first().group} components") }
            )
            HorizontalDivider()
        }
        items(
            items = entries,
            contentType = { _ -> "contentType2" },
            key = { it }
        ) { entry ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                ListItem(
                    overlineContent = { Text(entry.packageName) },
                    headlineContent = { Text(entry.name) },
                    supportingContent = {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(entry.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            entry.content()
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    }
}
