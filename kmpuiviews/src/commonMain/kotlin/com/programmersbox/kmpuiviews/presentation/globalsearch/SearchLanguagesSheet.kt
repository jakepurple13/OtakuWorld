package com.programmersbox.kmpuiviews.presentation.globalsearch

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.GlobalSearchLanguageOverride
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Lets the user pick which languages multi-language extensions search in during global search. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchLanguagesSheet(
    onDismiss: () -> Unit,
    sourceRepository: SourceRepository = koinInject(),
    settings: NewSettingsHandling = koinInject(),
) {
    val state by remember { searchLanguagesState(sourceRepository, settings) }
        .collectAsStateWithLifecycle(null)
    var editing by remember { mutableStateOf<String?>(null) }

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
                    if (targetState != null) {
                        // Sliding to the Right when going to OverrideContent
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeIn() togetherWith slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeOut()
                    } else {
                        // Sliding to the Left when returning to DefaultsContent
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        ) + fadeIn() togetherWith slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        ) + fadeOut()
                    }
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
    state: SearchLanguagesState,
    settings: NewSettingsHandling,
    onEdit: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Text("Search languages", style = MaterialTheme.typography.titleLarge)

    if (state.groups.isEmpty()) {
        Text(
            "None of your installed extensions offer more than one language.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    SectionLabel("Default")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.available.forEach { lang ->
            val selected = lang in state.defaults
            FilterChip(
                selected = selected,
                onClick = {
                    val updated = if (selected) state.defaults - lang else state.defaults + lang
                    // Keep at least one; an empty list means "device language".
                    if (updated.isNotEmpty()) scope.launch { settings.setGlobalSearchLanguages(updated.sorted()) }
                },
                label = { Text(lang) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, null) }
                } else null,
            )
        }
    }
    Text(
        "Applies to every extension below that uses Default. Single-language extensions are always searched.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    SectionLabel("Multi-language extensions")
    Column {
        state.groups.forEach { group ->
            val override = state.overrides[group.packageName]
            val chosen = SearchLanguages.effectiveLanguages(group.languages, state.defaults, override)
            val label = when {
                override?.searchAll == true -> "All languages"
                override != null -> chosen.joinToString().ifEmpty { "None" }
                else -> "Default · " + chosen.joinToString().ifEmpty { "No match" }
            }
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

private enum class OverrideMode { Default, All, Custom }

@Composable
private fun OverrideContent(
    group: SearchLanguageGroup,
    state: SearchLanguagesState,
    settings: NewSettingsHandling,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val override = state.overrides[group.packageName]
    val mode = when {
        override == null -> OverrideMode.Default
        override.searchAll -> OverrideMode.All
        else -> OverrideMode.Custom
    }

    fun save(value: GlobalSearchLanguageOverride?) {
        scope.launch { settings.setGlobalSearchLanguageOverride(group.packageName, value) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Text(group.name, style = MaterialTheme.typography.titleLarge)
    }

    SectionLabel("Search in")
    Column {
        listOf(
            OverrideMode.Default to "Default (${state.defaults.sorted().joinToString()})",
            OverrideMode.All to "All languages",
            OverrideMode.Custom to "Choose languages",
        ).forEach { (option, text) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = mode == option, role = Role.RadioButton) {
                        when (option) {
                            OverrideMode.Default -> save(null)
                            OverrideMode.All -> save(GlobalSearchLanguageOverride(searchAll = true))
                            OverrideMode.Custom -> if (mode != OverrideMode.Custom) {
                                // Start from what is searched now so switching changes nothing yet.
                                save(
                                    GlobalSearchLanguageOverride(
                                        languages = SearchLanguages.effectiveLanguages(group.languages, state.defaults, override)
                                    )
                                )
                            }
                        }
                    }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = mode == option, onClick = null)
                Text(text, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }

    if (override != null && !override.searchAll) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            group.languages.forEach { lang ->
                val selected = lang in override.languages
                FilterChip(
                    selected = selected,
                    onClick = {
                        save(
                            override.copy(
                                languages = if (selected) override.languages - lang else override.languages + lang
                            )
                        )
                    },
                    label = { Text(lang) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, null) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
