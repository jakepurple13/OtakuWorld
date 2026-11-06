package com.programmersbox.kmpuiviews.presentation.settings.updatesources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.us_check_all
import otakuworld.kmpuiviews.generated.resources.us_checking
import otakuworld.kmpuiviews.generated.resources.us_description
import otakuworld.kmpuiviews.generated.resources.us_failed_last_check
import otakuworld.kmpuiviews.generated.resources.us_favorites
import otakuworld.kmpuiviews.generated.resources.us_no_favorites
import otakuworld.kmpuiviews.generated.resources.us_no_matches
import otakuworld.kmpuiviews.generated.resources.us_none_checked
import otakuworld.kmpuiviews.generated.resources.us_none_skipped
import otakuworld.kmpuiviews.generated.resources.us_search
import otakuworld.kmpuiviews.generated.resources.us_skip_all
import otakuworld.kmpuiviews.generated.resources.us_skipped
import otakuworld.kmpuiviews.generated.resources.us_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSourcesScreen(
    viewModel: UpdateSourcesViewModel = koinViewModel(),
) {
    val navActions = LocalNavActions.current
    val scope = rememberCoroutineScope()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    HideNavBarWhileOnScreen()

    fun change(sources: List<String>, skipped: Boolean) {
        scope.launch { viewModel.setSkipped(sources, skipped) }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.us_title)) },
                navigationIcon = {
                    IconButton(onClick = { navActions.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val all = rows ?: return@Scaffold
        val visible = if (query.isBlank()) all else all.filter { it.name.contains(query.trim(), ignoreCase = true) }
        val checking = visible.filterNot { it.skipped }
        val skipped = visible.filter { it.skipped }

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "description") {
                Text(
                    stringResource(Res.string.us_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (all.isEmpty()) {
                item(key = "empty") {
                    EmptyText(stringResource(Res.string.us_no_favorites), Modifier.animateItem())
                }
                return@LazyColumn
            }

            item(key = "search") {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(Res.string.us_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (visible.isEmpty()) {
                item(key = "no_matches") {
                    EmptyText(stringResource(Res.string.us_no_matches, query.trim()), Modifier.animateItem())
                }
                return@LazyColumn
            }

            group(
                key = "checking",
                title = { stringResource(Res.string.us_checking, checking.size) },
                bulkLabel = { stringResource(Res.string.us_skip_all) },
                onBulk = {
                    change(checking.map { it.source }, skipped = true)
                },
                emptyText = { stringResource(Res.string.us_none_checked) },
                rows = checking,
                onToggle = { row ->
                    change(listOf(row.source), skipped = true)
                },
            )

            group(
                key = "skipped",
                title = { stringResource(Res.string.us_skipped, skipped.size) },
                bulkLabel = { stringResource(Res.string.us_check_all) },
                onBulk = {
                    change(skipped.map { it.source }, skipped = false)
                },
                emptyText = { stringResource(Res.string.us_none_skipped) },
                rows = skipped,
                onToggle = { row ->
                    change(listOf(row.source), skipped = false)
                },
            )
        }
    }
}

private fun LazyListScope.group(
    key: String,
    title: @Composable () -> String,
    bulkLabel: @Composable () -> String,
    onBulk: () -> Unit,
    emptyText: @Composable () -> String,
    rows: List<UpdateSourceRow>,
    onToggle: (UpdateSourceRow) -> Unit,
) {
    item(key = "header_$key") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)
        ) {
            Text(
                title(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBulk, enabled = rows.isNotEmpty()) { Text(bulkLabel()) }
        }
    }

    if (rows.isEmpty()) {
        item(key = "empty_$key") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    emptyText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
        return
    }

    // Keyed by source, so a row slides between the groups when it is toggled.
    itemsIndexed(rows, key = { _, row -> row.source }) { index, row ->
        val top = if (index == 0) 20.dp else 4.dp
        val bottom = if (index == rows.lastIndex) 20.dp else 4.dp
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 1.dp)
        ) {
            SourceRow(row = row, onToggle = { onToggle(row) })
        }
    }
}

@Composable
private fun SourceRow(row: UpdateSourceRow, onToggle: () -> Unit) {
    val favorites = pluralStringResource(Res.plurals.us_favorites, row.favorites, row.favorites)
    ListItem(
        leadingContent = {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(40.dp)
                    .alpha(if (row.skipped) 0.5f else 1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        row.name.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        },
        headlineContent = {
            Text(
                row.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (row.skipped) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(favorites)
                if (row.failedLastCheck) {
                    Text("·")
                    Text(stringResource(Res.string.us_failed_last_check), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        trailingContent = {
            Switch(checked = !row.skipped, onCheckedChange = { onToggle() })
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onToggle)
    )
}

@Composable
private fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 32.dp)
    )
}
