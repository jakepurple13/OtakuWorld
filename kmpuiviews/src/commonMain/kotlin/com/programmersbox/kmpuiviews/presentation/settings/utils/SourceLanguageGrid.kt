package com.programmersbox.kmpuiviews.presentation.settings.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpmodels.KmpSourceInformation

/** Short label for a source's language, e.g. "EN" or "PT-BR", or its name when it has no language. */
internal fun KmpSourceInformation.languageCode() = apiService.language?.uppercase() ?: apiService.displayName

/** Opens [SourceLanguageGridSheet] for a multi-language extension, showing the language that a row tap picks. */
@Composable
internal fun LanguagePill(code: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(code, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = Modifier.widthIn(max = 128.dp),
        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Choose language") },
    )
}

/**
 * Every language of one extension as a vertically scrolling grid. Columns adapt to the width, so
 * it never scrolls sideways. Preferred languages come first, then the rest in [sources] order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SourceLanguageGridSheet(
    title: String,
    sources: List<KmpSourceInformation>,
    currentService: String?,
    isPreferred: (KmpSourceInformation) -> Boolean,
    onDismiss: () -> Unit,
    onClick: (KmpSourceInformation) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val (preferred, others) = remember(sources, query) {
        sources
            .filter {
                query.isBlank() ||
                    it.apiService.language.orEmpty().contains(query, ignoreCase = true) ||
                    it.tileName().contains(query, ignoreCase = true)
            }
            .partition(isPreferred)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Column {
                        Text(title)
                        Text(
                            "${sources.size} languages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                actions = actions,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search languages") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                if (preferred.isNotEmpty()) {
                    header("Your languages")
                    tiles(preferred, currentService, onClick)
                    if (others.isNotEmpty()) header("Other languages")
                }
                tiles(others, currentService, onClick)
                if (preferred.isEmpty() && others.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "No language matches \"$query\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.header(text: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun LazyGridScope.tiles(
    sources: List<KmpSourceInformation>,
    currentService: String?,
    onClick: (KmpSourceInformation) -> Unit,
) {
    items(sources) { source ->
        val selected = source.apiService.serviceName == currentService
        Surface(
            onClick = { onClick(source) },
            shape = MaterialTheme.shapes.large,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Text(
                    source.languageCode(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    source.tileName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun KmpSourceInformation.tileName() = languageName() ?: apiService.displayName
