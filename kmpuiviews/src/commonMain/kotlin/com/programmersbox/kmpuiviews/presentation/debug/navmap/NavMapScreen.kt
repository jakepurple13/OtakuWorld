package com.programmersbox.kmpuiviews.presentation.debug.navmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import org.koin.compose.viewmodel.koinViewModel

/** Debug-only map of every screen in this app and every place that navigates to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavMapScreen(viewModel: NavMapViewModel = koinViewModel()) {
    HideNavBarWhileOnScreen()
    val navActions = LocalNavActions.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Bumped on every search edit / "Show on map" so the canvas re-centres even on the same target.
    var centerRequest by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val canOpen: (NavNode) -> Boolean = { n -> !n.pseudo && n.fqcn != null && createNavKey(n.fqcn, n.args) != null }
    val open: (NavNode) -> Unit = { n -> n.fqcn?.let { createNavKey(it, n.args) }?.let { navActions.navigate(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Navigation map")
                        state.graph?.let {
                            Text(it.app, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navActions.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        val graph = state.graph
        val layout = state.layout
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }

            graph == null || layout == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("No navigation graph in this build", style = MaterialTheme.typography.bodyLarge)
            }

            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = {
                        viewModel.setQuery(it)
                        viewModel.firstMatch()?.let { id -> centerRequest = id to ((centerRequest?.second ?: 0) + 1) }
                    },
                    placeholder = { Text("Find screen… e.g. Reader") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(state.showEntryPoints, viewModel::toggleEntryPoints, { Text("Entry points") })
                    FilterChip(state.showArgs, viewModel::toggleArgs, { Text("Show args") })
                    FilterChip(state.overridesOnly, viewModel::toggleOverridesOnly, { Text("Overrides only") })
                    FilterChip(
                        state.orphansOnly, viewModel::toggleOrphansOnly, { Text("No way in") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    )
                }
                val orphans = graph.orphans
                Text(
                    "${graph.nodes.count { !it.pseudo }} screens · ${graph.edges.size} edges · " +
                        "${graph.edges.count { it.isOverride }} overrides · ${orphans.size} no way in · ${graph.unresolved.size} unresolved",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                )
                SecondaryTabRow(selectedTabIndex = state.mode.ordinal) {
                    NavMapMode.entries.forEach { mode ->
                        Tab(
                            selected = state.mode == mode,
                            onClick = { viewModel.setMode(mode) },
                            text = { Text(mode.name) },
                        )
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    when (state.mode) {
                        NavMapMode.Map -> {
                            NavMapCanvas(
                                state = state,
                                graph = graph,
                                layout = layout,
                                searchTarget = centerRequest?.first,
                                centerKey = centerRequest?.second ?: 0,
                                onSelect = viewModel::select,
                                modifier = Modifier.fillMaxSize(),
                            )
                            SelectionOverlay(
                                selected = state.selected?.let { id -> graph.nodes.firstOrNull { it.id == id } },
                                graph = graph,
                                canOpen = canOpen,
                                onFocus = viewModel::focus,
                                onOpen = open,
                                onClose = { viewModel.select(null) },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                            )
                        }

                        NavMapMode.Focus -> NavMapFocus(
                            state = state,
                            graph = graph,
                            onWalk = viewModel::walk,
                            onJumpTrail = viewModel::jumpTrail,
                            onOpen = open,
                            canOpen = canOpen,
                            onShowOnMap = { id ->
                                viewModel.select(id)
                                viewModel.setMode(NavMapMode.Map)
                                centerRequest = id to ((centerRequest?.second ?: 0) + 1)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionOverlay(
    selected: NavNode?,
    graph: NavGraph,
    canOpen: (NavNode) -> Boolean,
    onFocus: (String) -> Unit,
    onOpen: (NavNode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep showing the last node while the card slides out.
    var shown by remember { mutableStateOf<NavNode?>(null) }
    if (selected != null) shown = selected
    AnimatedVisibility(
        visible = selected != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        val node = shown ?: return@AnimatedVisibility
        SelectionCard(
            node = node,
            graph = graph,
            canOpen = canOpen(node),
            onFocus = { onFocus(node.id) },
            onOpen = { onOpen(node) },
            onClose = onClose,
        )
    }
}

@Composable
private fun SelectionCard(
    node: NavNode,
    graph: NavGraph,
    canOpen: Boolean,
    onFocus: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(node.label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            if (node.args.isNotEmpty()) {
                Text(
                    "(" + node.args.joinToString { it.name + if (it.nullable) "?" else "" } + ")",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val ins = graph.inbound(node.id).size
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("← $ins in", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                Text("→ ${graph.outbound(node.id).size} out", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                Text(
                    when {
                        node.pseudo -> "entry point"
                        node.args.isEmpty() -> "data object"
                        else -> "data class"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!node.pseudo && ins == 0) {
                Text(
                    if (node.allowOrphan) "Nothing navigates here (allowed in overrides)." else "Nothing navigates here.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onFocus) { Text("Focus") }
                if (!node.pseudo) OutlinedButton(onClick = onOpen, enabled = canOpen) { Text("Open") }
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
    }
}
