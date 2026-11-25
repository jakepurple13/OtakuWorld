@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One row of the Tabs & browsing page. [content] gets the lazy item's modifier and its place in the group. */
internal class SettingsRow(val key: String, val content: @Composable (Modifier, ListItemShapes) -> Unit)

/** A group header, then its rows as one segmented list. Hidden rows are left out, so the shapes stay right. */
internal fun LazyListScope.settingsGroup(title: String, rows: List<SettingsRow>) {
    if (rows.isEmpty()) return
    item(key = "header/$title", contentType = "tabsSettingsHeader") {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .animateItem()
                .padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
    rows.forEachIndexed { i, row ->
        item(key = row.key, contentType = "tabsSettingsRow") {
            row.content(Modifier.animateItem(), ListItemDefaults.segmentedShapes(index = i, count = rows.size))
        }
    }
}

@Composable
private fun rowColors() = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

/** Segmented buttons under the row's text. Clicking the row itself moves to the next choice. */
internal fun <T> choiceRow(
    key: String,
    title: String,
    supporting: String,
    icon: ImageVector,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) = SettingsRow(key) { modifier, shapes ->
    SegmentedListItem(
        onClick = {
            val i = options.indexOfFirst { it.first == selected }
            onSelect(options[(i + 1) % options.size].first)
        },
        shapes = shapes,
        colors = rowColors(),
        leadingContent = { Icon(icon, null) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(supporting)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = value == selected,
                            onClick = { onSelect(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
            }
        },
        modifier = modifier,
    ) { Text(title) }
}

internal fun switchRow(
    key: String,
    title: String,
    supporting: String,
    icon: ImageVector,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) = SettingsRow(key) { modifier, shapes ->
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onChecked,
        shapes = shapes,
        colors = rowColors(),
        leadingContent = { Icon(icon, null) },
        supportingContent = { Text(supporting) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = modifier,
    ) { Text(title) }
}

/** The current choice on the trailing side; clicking the row opens the list of choices. */
internal fun <T> menuRow(
    key: String,
    title: String,
    supporting: String,
    icon: ImageVector,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) = SettingsRow(key) { modifier, shapes ->
    var open by remember { mutableStateOf(false) }
    SegmentedListItem(
        onClick = { open = true },
        shapes = shapes,
        colors = rowColors(),
        leadingContent = { Icon(icon, null) },
        supportingContent = { Text(supporting) },
        trailingContent = {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(options.first { it.first == selected }.second, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                open = false
                                onSelect(value)
                            },
                        )
                    }
                }
            }
        },
        modifier = modifier,
    ) { Text(title) }
}

internal fun linkRow(
    key: String,
    title: String,
    supporting: String,
    icon: ImageVector,
    trailing: ImageVector,
    onClick: () -> Unit,
) = SettingsRow(key) { modifier, shapes ->
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = rowColors(),
        leadingContent = { Icon(icon, null) },
        supportingContent = { Text(supporting) },
        trailingContent = { Icon(trailing, null) },
        modifier = modifier,
    ) { Text(title) }
}

/** A non-clickable row holding its own controls, shaped like the rows around it. */
@Composable
private fun PanelRow(
    title: String,
    supporting: String,
    icon: ImageVector,
    shapes: ListItemShapes,
    modifier: Modifier,
    control: @Composable () -> Unit,
) {
    Surface(shape = shapes.shape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                control()
            }
        }
    }
}

/** The new tab page's sections: each can be turned on or off and moved up or down. */
internal fun sectionsRow(
    key: String,
    title: String,
    supporting: String,
    icon: ImageVector,
    sections: List<SectionSetting>,
    label: (NewTabSection) -> String,
    onChange: (List<SectionSetting>) -> Unit,
) = SettingsRow(key) { modifier, shapes ->
    PanelRow(title, supporting, icon, shapes, modifier) {
        Column(Modifier.padding(top = 4.dp)) {
            sections.forEachIndexed { i, setting ->
                fun swap(with: Int) = onChange(sections.toMutableList().also { it[i] = it[with]; it[with] = setting })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.size(24.dp))
                    Text(
                        label(setting.section),
                        color = if (setting.on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { swap(i - 1) }, enabled = i > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, "Move ${label(setting.section)} up")
                    }
                    IconButton(onClick = { swap(i + 1) }, enabled = i < sections.lastIndex) {
                        Icon(Icons.Default.KeyboardArrowDown, "Move ${label(setting.section)} down")
                    }
                    Switch(
                        checked = setting.on,
                        onCheckedChange = { on -> onChange(sections.toMutableList().also { it[i] = setting.copy(on = on) }) },
                        modifier = Modifier.semantics { contentDescription = "Show ${label(setting.section)}" },
                    )
                }
            }
        }
    }
}

/** Four corners in a 2 × 2 grid, like the window they stand for. */
internal fun cornerRow(
    key: String,
    title: String,
    supporting: String,
    icon: ImageVector,
    selected: MiniPlayerCorner,
    onSelect: (MiniPlayerCorner) -> Unit,
) = SettingsRow(key) { modifier, shapes ->
    PanelRow(title, supporting, icon, shapes, modifier) {
        val names = mapOf(
            MiniPlayerCorner.TopLeft to "Top left",
            MiniPlayerCorner.TopRight to "Top right",
            MiniPlayerCorner.BottomLeft to "Bottom left",
            MiniPlayerCorner.BottomRight to "Bottom right",
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            MiniPlayerCorner.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    pair.forEach { corner ->
                        val on = corner == selected
                        Surface(
                            onClick = { onSelect(corner) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier
                                .size(width = 44.dp, height = 28.dp)
                                .semantics { contentDescription = names.getValue(corner) },
                        ) {}
                    }
                }
            }
            Text(names.getValue(selected), style = MaterialTheme.typography.labelMedium)
        }
    }
}
