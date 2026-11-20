package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import java.util.Locale

@Serializable
data object MotionSettingsRoute : NavKey

private const val HOVER_PREVIEW_DELAY_MILLIS = 180L
private const val HOVER_REVERT_DELAY_MILLIS = 400L
private val SplitBreakpoint = 640.dp

@Composable
fun MotionSettingsScreen() {
    val catalog = getKoin().getOrNull<DesktopMotionCatalog>()
    if (catalog == null) {
        MotionUnavailableContent()
        return
    }
    val settings = koinInject<MangaDesktopSettings>()
    var launchKey by settings.launchAnimation.asState()
    var quitKey by settings.quitAnimation.asState()
    MotionSettingsContent(
        catalog = catalog,
        launchKey = launchKey,
        quitKey = quitKey,
        onLaunchKey = { launchKey = it },
        onQuitKey = { quitKey = it },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotionUnavailableContent() {
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Launch & quit animations") },
                navigationIcon = { BackButton() },
            )
        },
    ) { padding ->
        Text("No animations available", Modifier.padding(padding).padding(16.dp))
    }
}

/** Testable body: no Koin, no DataStore. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)
@Composable
fun MotionSettingsContent(
    catalog: DesktopMotionCatalog,
    launchKey: String,
    quitKey: String,
    onLaunchKey: (String) -> Unit,
    onQuitKey: (String) -> Unit,
) {
    var phase by remember { mutableStateOf(MotionPhase.Launch) }
    val selected = catalog.resolve(phase, if (phase == MotionPhase.Launch) launchKey else quitKey)
    val options = catalog.options(phase)

    // Hovering a row previews it after a short delay; leaving reverts to the saved choice.
    var hoveredKey by remember { mutableStateOf<String?>(null) }
    // A clicked row stays previewed until the saved choice catches up, so the preview never flicks back meanwhile.
    var clickedKey by remember(phase) { mutableStateOf<String?>(null) }
    LaunchedEffect(selected.key) { if (clickedKey == selected.key) clickedKey = null }
    val previewed = options.firstOrNull { it.key == (hoveredKey ?: clickedKey) } ?: selected
    val scope = rememberCoroutineScope()
    var hoverJob by remember { mutableStateOf<Job?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    )

    fun LazyListScope.optionGroup(label: String, items: List<DesktopMotion>) {
        item(key = "header/$label", contentType = "motionGroupHeader") {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .animateItem()
                    .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            )
        }
        items(items.size, key = { "$label/${items[it].key}" }, contentType = { "motionOption" }) { i ->
            val motion = items[i]
            SegmentedListItem(
                selected = motion.key == selected.key,
                onClick = {
                    hoverJob?.cancel()
                    hoveredKey = null
                    clickedKey = motion.key
                    if (phase == MotionPhase.Launch) onLaunchKey(motion.key) else onQuitKey(motion.key)
                },
                content = { Text(motion.title) },
                supportingContent = { Text(motion.subtitle(phase)) },
                leadingContent = { RadioButton(selected = motion.key == selected.key, onClick = null) },
                colors = colors,
                shapes = ListItemDefaults.segmentedShapes(index = i, count = items.size),
                modifier = Modifier
                    .animateItem()
                    .onPointerEvent(PointerEventType.Enter) {
                        hoverJob?.cancel()
                        hoverJob = scope.launch {
                            delay(HOVER_PREVIEW_DELAY_MILLIS)
                            hoveredKey = motion.key
                        }
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        hoverJob?.cancel()
                        hoverJob = scope.launch {
                            delay(HOVER_REVERT_DELAY_MILLIS)
                            hoveredKey = null
                        }
                    },
            )
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Launch & quit animations") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val tabs: LazyListScope.() -> Unit = {
                item(key = "tabs", contentType = "motionTabs") {
                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .animateItem()
                            .padding(bottom = 8.dp)
                    ) {
                        MotionPhase.entries.forEachIndexed { index, p ->
                            SegmentedButton(
                                selected = phase == p,
                                onClick = {
                                    hoverJob?.cancel()
                                    hoveredKey = null
                                    phase = p
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, MotionPhase.entries.size),
                                label = { Text(if (p == MotionPhase.Launch) "Launch" else "Quit") },
                            )
                        }
                    }
                }
            }
            val list: LazyListScope.() -> Unit = {
                optionGroup("Basic", options.take(2))
                optionGroup(catalog.appName, options.drop(2))
            }
            if (maxWidth >= SplitBreakpoint) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                    ) {
                        tabs()
                        list()
                    }
                    MotionPreview(previewed, Modifier.weight(1.2f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    item(key = "preview", contentType = "motionPreview") {
                        MotionPreview(previewed, Modifier.animateItem())
                    }
                    tabs()
                    list()
                }
            }
        }
    }
}

/** Off has fixed copy; others show their duration, with a second decimal only when needed (0.45 s, 1.2 s). */
private fun DesktopMotion.subtitle(phase: MotionPhase): String = when {
    this === OffMotion -> if (phase == MotionPhase.Launch) "Opens straight to the app" else "Closes right away"
    durationMillis % 100 == 0 -> String.format(Locale.US, "%.1f s", durationMillis / 1000f)
    else -> String.format(Locale.US, "%.2f s", durationMillis / 1000f)
}
