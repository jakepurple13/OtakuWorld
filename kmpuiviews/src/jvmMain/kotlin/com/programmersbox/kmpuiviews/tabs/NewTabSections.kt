package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard
import com.programmersbox.kmpuiviews.presentation.tabs.tabTarget
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.koin.compose.koinInject

/** A small uppercase label above each section, as in the mockup. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Section(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        content()
    }
}

/** Submitting opens (or switches to) the Search tab with the query. */
@Composable
internal fun NewTabSearch(focusOnFirstShow: Boolean, modifier: Modifier) {
    val navActions = LocalNavActions.current
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    // Saved with the tab, so coming back to a parked new tab does not grab the cursor again.
    var shown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (focusOnFirstShow && !shown) focusRequester.requestFocus()
        shown = true
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search all sources") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { query.trim().takeIf { it.isNotEmpty() }?.let { navActions.globalSearch(it) } }
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

@Composable
internal fun NewTabContinue(items: List<RecentModel>, modifier: Modifier) {
    val navActions = LocalNavActions.current
    Section("Continue reading", modifier) {
        if (items.isEmpty()) {
            EmptyLine("Nothing read yet")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.url }) { item ->
                    M3CoverCard(
                        imageUrl = item.imageUrl,
                        name = item.title,
                        placeHolder = { painterLogo() },
                        onClick = {
                            navActions.details(
                                title = item.title,
                                description = item.description,
                                url = item.url,
                                imageUrl = item.imageUrl,
                                source = item.source,
                            )
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun NewTabUpdates(items: List<NotificationItem>, modifier: Modifier) {
    val navActions = LocalNavActions.current
    Section("Favorites with new chapters", modifier) {
        if (items.isEmpty()) {
            EmptyLine("No new chapters")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    val open = {
                        navActions.details(
                            title = item.notiTitle,
                            description = item.summaryText,
                            url = item.url,
                            imageUrl = item.imageUrl.orEmpty(),
                            source = item.source,
                        )
                    }
                    AssistChip(
                        onClick = open,
                        modifier = Modifier.tabTarget(open),
                        label = { Text(item.notiTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.NewReleases, null) },
                    )
                }
            }
        }
    }
}

/** The controller's closed stack, newest first; a click reopens that tab in place of this new tab page. */
@Composable
internal fun NewTabClosed(
    modifier: Modifier,
    controller: TabsController = koinInject(),
    keys: TabKeys = koinInject(),
) {
    val tabId = LocalTabId.current
    val closed = controller.recentlyClosed.take(MAX_CLOSED)
    Section("Recently closed", modifier) {
        if (closed.isEmpty()) {
            EmptyLine("No closed tabs")
        } else {
            Column {
                closed.forEach { entry ->
                    val label = keys.labelOf(entry.current)
                    ListItem(
                        headlineContent = { Text(label.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(label.icon ?: Icons.Default.Restore, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            val window = tabId?.let { controller.windowOf(it) } ?: controller.windows.first()
                            controller.reopenClosed(window.id, entry, replacing = tabId)
                        },
                    )
                }
            }
        }
    }
}

private const val MAX_CLOSED = 5

@Composable
internal fun NewTabLinks(modifier: Modifier, controller: TabsController = koinInject()) {
    val navActions = LocalNavActions.current
    Section("Quick links", modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LinkChip("Lists", Icons.AutoMirrored.Default.List) { navActions.customList() }
            controller.downloadsKey?.let { key ->
                LinkChip("Downloads", Icons.Default.Download) { navActions.navigate(key) }
            }
            LinkChip("Settings", Icons.Default.Settings) { navActions.navigate(Screen.Settings) }
            LinkChip("History", Icons.Default.History) { navActions.history() }
        }
    }
}

@Composable
private fun LinkChip(text: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(text) }, leadingIcon = { Icon(icon, null) })
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
