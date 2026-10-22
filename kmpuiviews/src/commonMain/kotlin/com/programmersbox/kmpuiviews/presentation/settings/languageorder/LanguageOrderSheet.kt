package com.programmersbox.kmpuiviews.presentation.settings.languageorder

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SourceLanguageOrder
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.globalsearch.SearchLanguageGroup
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableColumn

/**
 * Lets the user order the languages of multi-language extensions in the source chooser.
 * [startAt] opens straight into one extension's order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageOrderSheet(
    onDismiss: () -> Unit,
    startAt: String? = null,
    sourceRepository: SourceRepository = koinInject(),
    settings: NewSettingsHandling = koinInject(),
) {
    val state by remember { languageOrderState(sourceRepository, settings) }
        .collectAsStateWithLifecycle(null)
    var editing by remember { mutableStateOf(startAt) }

    // An empty sheet dismisses itself, so wait for the first state before showing it.
    val s = state ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(SheetValue.Expanded),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val group = s.groups.find { it.packageName == editing }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .animateContentSize()
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedContent(
                group,
                transitionSpec = {
                    val towards = if (targetState != null) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    slideIntoContainer(towards, tween(300)) + fadeIn() togetherWith
                        slideOutOfContainer(towards, tween(300)) + fadeOut()
                }
            ) { target ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (target == null) {
                        DefaultsContent(s, settings) { editing = it }
                    } else {
                        OverrideContent(target, s, settings) { editing = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultsContent(
    state: LanguageOrderState,
    settings: NewSettingsHandling,
    onEdit: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    fun save(languages: List<String>) {
        scope.launch { settings.setSourceLanguageOrder(languages) }
    }

    Text("Language order", style = MaterialTheme.typography.titleLarge)

    if (state.groups.isEmpty()) {
        Text(
            "None of your installed extensions offer more than one language.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SectionLabel("Default order")
    LanguageList(
        languages = state.defaults,
        onReorder = ::save,
        // Keep at least one; an empty list means "device language".
        onRemove = if (state.defaults.size > 1) {
            { lang -> save(state.defaults - lang) }
        } else null,
    )

    if (state.unranked.isNotEmpty()) {
        SectionLabel("Add a language")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.unranked.forEach { lang ->
                AssistChip(
                    onClick = { save(state.defaults + lang) },
                    label = { Text(lang) },
                    leadingIcon = { Icon(Icons.Default.Add, null) },
                )
            }
        }
    }
    Text(
        "Languages not in the list come after it, A–Z.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    SectionLabel("Multi-language extensions")
    Column {
        state.groups.forEach { group ->
            val custom = state.overrides[group.packageName]
            val order = SourceLanguageOrdering.order(group.languages, state.orderFor(group.packageName))
            val label = (if (custom != null) "Custom · " else "Default · ") + order.take(2).joinToString()
            ListItem(
                headlineContent = { Text(group.name) },
                supportingContent = { Text("${group.languages.size} languages") },
                trailingContent = { SuggestionChip(onClick = { onEdit(group.packageName) }, label = { Text(label) }) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.selectable(selected = false, role = Role.Button) { onEdit(group.packageName) },
            )
        }
    }
}

@Composable
private fun OverrideContent(
    group: SearchLanguageGroup,
    state: LanguageOrderState,
    settings: NewSettingsHandling,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val custom = state.overrides[group.packageName]

    fun save(value: SourceLanguageOrder?) {
        scope.launch { settings.setSourceLanguageOrderOverride(group.packageName, value) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Text(group.name, style = MaterialTheme.typography.titleLarge)
    }

    SectionLabel("Order")
    Column {
        listOf(
            false to "Default (${state.defaults.joinToString()})",
            true to "Custom order",
        ).forEach { (isCustom, text) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = (custom != null) == isCustom, role = Role.RadioButton) {
                        when {
                            !isCustom -> save(null)
                            // Start from the current order so switching moves nothing yet.
                            custom == null -> save(
                                SourceLanguageOrder(languages = SourceLanguageOrdering.order(group.languages, state.defaults))
                            )
                        }
                    }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = (custom != null) == isCustom, onClick = null)
                Text(text, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }

    if (custom != null) {
        LanguageList(
            // Languages the extension added since the order was saved go to the end.
            languages = SourceLanguageOrdering.order(group.languages, custom.languages),
            onReorder = { save(SourceLanguageOrder(languages = it)) },
        )
    }
}

/** Draggable, numbered list of languages. */
@Composable
internal fun LanguageList(
    languages: List<String>,
    onReorder: (List<String>) -> Unit,
    onRemove: ((String) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    // Held locally so a dropped item stays put while the new order saves.
    var items by remember(languages) { mutableStateOf(languages) }
    ReorderableColumn(
        list = items,
        onSettle = { from, to ->
            items = items.toMutableList().apply { add(to, removeAt(from)) }
            onReorder(items)
        },
        onMove = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { index, lang, isDragging ->
        key(lang) {
            ReorderableItem {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 6.dp else 0.dp),
                ) {
                    ListItem(
                        leadingContent = {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.primary)
                        },
                        headlineContent = { Text(languageDisplayName(lang)) },
                        supportingContent = { Text(lang) },
                        trailingContent = {
                            Row {
                                onRemove?.let {
                                    IconButton(onClick = { it(lang) }) { Icon(Icons.Default.Close, "Remove $lang") }
                                }
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                    ),
                                ) { Icon(Icons.Rounded.DragHandle, "Reorder") }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
