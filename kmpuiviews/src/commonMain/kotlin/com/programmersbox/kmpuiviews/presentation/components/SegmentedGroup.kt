package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupMarker

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedGroup(
    modifier: Modifier = Modifier,
    content: SegmentedGroupScope.() -> Unit,
) {
    val updatedContent by rememberUpdatedState(content)
    val categoryGroup = remember(updatedContent) {
        SegmentedGroupImpl(updatedContent)
    }
    val stateHolder = rememberSaveableStateHolder()

    val segmentedColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        for (i in 0 until categoryGroup.size) {
            stateHolder.SaveableStateProvider(i) {
                when (val item = categoryGroup.get(i)) {
                    is SegmentedGroupItem.SegmentedGroupListItem -> {
                        SegmentedListItem(
                            modifier = item.modifier,
                            enabled = item.enabled,
                            leadingContent = item.leadingContent,
                            trailingContent = item.trailingContent,
                            overlineContent = item.overlineContent,
                            supportingContent = item.supportingContent,
                            verticalAlignment = item.verticalAlignment ?: ListItemDefaults.verticalAlignment(),
                            onLongClick = item.onLongClick,
                            onLongClickLabel = item.onLongClickLabel,
                            colors = item.colors ?: segmentedColors,
                            elevation = item.elevation,
                            contentPadding = item.contentPadding,
                            interactionSource = item.interactionSource,
                            content = item.content,
                            onClick = item.onClick,
                            shapes = ListItemDefaults.segmentedShapes(i, categoryGroup.size)
                        )
                    }

                    is SegmentedGroupItem.SegmentedSwitchItem -> {
                        SegmentedListItem(
                            modifier = item.modifier,
                            enabled = item.enabled,
                            leadingContent = item.leadingContent,
                            trailingContent = item.trailingContent,
                            overlineContent = item.overlineContent,
                            supportingContent = item.supportingContent,
                            verticalAlignment = item.verticalAlignment ?: ListItemDefaults.verticalAlignment(),
                            onLongClick = item.onLongClick,
                            onLongClickLabel = item.onLongClickLabel,
                            colors = item.colors ?: segmentedColors,
                            elevation = item.elevation,
                            contentPadding = item.contentPadding,
                            interactionSource = item.interactionSource,
                            content = item.content,
                            checked = item.checked,
                            onCheckedChange = item.onCheckedChange,
                            shapes = ListItemDefaults.segmentedShapes(i, categoryGroup.size)
                        )
                    }
                }
            }
        }
    }
}

@CategoryGroupMarker
interface SegmentedGroupScope {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun segmentedListItem(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        leadingContent: @Composable (() -> Unit)? = null,
        trailingContent: @Composable (() -> Unit)? = null,
        overlineContent: @Composable (() -> Unit)? = null,
        supportingContent: @Composable (() -> Unit)? = null,
        verticalAlignment: Alignment.Vertical? = null,
        onLongClick: (() -> Unit)? = null,
        onLongClickLabel: String? = null,
        colors: ListItemColors? = null,
        elevation: ListItemElevation = ListItemDefaults.elevation(),
        contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
        interactionSource: MutableInteractionSource? = null,
        content: @Composable () -> Unit,
    )

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun segmentedSwitchItem(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        leadingContent: @Composable (() -> Unit)? = null,
        trailingContent: @Composable (() -> Unit)? = null,
        overlineContent: @Composable (() -> Unit)? = null,
        supportingContent: @Composable (() -> Unit)? = null,
        verticalAlignment: Alignment.Vertical? = null,
        onLongClick: (() -> Unit)? = null,
        onLongClickLabel: String? = null,
        colors: ListItemColors? = null,
        elevation: ListItemElevation = ListItemDefaults.elevation(),
        contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
        interactionSource: MutableInteractionSource? = null,
        content: @Composable () -> Unit,
    )
}

private class SegmentedGroupImpl(
    content: SegmentedGroupScope.() -> Unit = {},
) : SegmentedGroupScope {
    val intervals: MutableIntervalList<SegmentedGroupItem> = MutableIntervalList()

    val size: Int get() = intervals.size

    fun get(index: Int): SegmentedGroupItem = intervals[index].value

    init {
        apply(content)
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun segmentedListItem(
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        leadingContent: @Composable (() -> Unit)?,
        trailingContent: @Composable (() -> Unit)?,
        overlineContent: @Composable (() -> Unit)?,
        supportingContent: @Composable (() -> Unit)?,
        verticalAlignment: Alignment.Vertical?,
        onLongClick: (() -> Unit)?,
        onLongClickLabel: String?,
        colors: ListItemColors?,
        elevation: ListItemElevation,
        contentPadding: PaddingValues,
        interactionSource: MutableInteractionSource?,
        content: @Composable (() -> Unit),
    ) {
        intervals.addInterval(
            1,
            SegmentedGroupItem.SegmentedGroupListItem(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                overlineContent = overlineContent,
                supportingContent = supportingContent,
                verticalAlignment = verticalAlignment,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
                colors = colors,
                elevation = elevation,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content,
            )
        )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun segmentedSwitchItem(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        leadingContent: @Composable (() -> Unit)?,
        trailingContent: @Composable (() -> Unit)?,
        overlineContent: @Composable (() -> Unit)?,
        supportingContent: @Composable (() -> Unit)?,
        verticalAlignment: Alignment.Vertical?,
        onLongClick: (() -> Unit)?,
        onLongClickLabel: String?,
        colors: ListItemColors?,
        elevation: ListItemElevation,
        contentPadding: PaddingValues,
        interactionSource: MutableInteractionSource?,
        content: @Composable (() -> Unit),
    ) {
        intervals.addInterval(
            1,
            SegmentedGroupItem.SegmentedSwitchItem(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                overlineContent = overlineContent,
                supportingContent = supportingContent,
                verticalAlignment = verticalAlignment,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
                colors = colors,
                elevation = elevation,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content,
            )
        )
    }
}

private sealed class SegmentedGroupItem {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    data class SegmentedGroupListItem(
        val onClick: () -> Unit,
        val modifier: Modifier = Modifier,
        val enabled: Boolean = true,
        val leadingContent: @Composable (() -> Unit)? = null,
        val trailingContent: @Composable (() -> Unit)? = null,
        val overlineContent: @Composable (() -> Unit)? = null,
        val supportingContent: @Composable (() -> Unit)? = null,
        val verticalAlignment: Alignment.Vertical? = null,
        val onLongClick: (() -> Unit)? = null,
        val onLongClickLabel: String? = null,
        val colors: ListItemColors? = null,
        val elevation: ListItemElevation = ListItemDefaults.elevation(),
        val contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
        val interactionSource: MutableInteractionSource? = null,
        val content: @Composable () -> Unit,
    ) : SegmentedGroupItem()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    data class SegmentedSwitchItem(
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val modifier: Modifier = Modifier,
        val enabled: Boolean = true,
        val leadingContent: @Composable (() -> Unit)? = null,
        val trailingContent: @Composable (() -> Unit)? = null,
        val overlineContent: @Composable (() -> Unit)? = null,
        val supportingContent: @Composable (() -> Unit)? = null,
        val verticalAlignment: Alignment.Vertical? = null,
        val onLongClick: (() -> Unit)? = null,
        val onLongClickLabel: String? = null,
        val colors: ListItemColors? = null,
        val elevation: ListItemElevation = ListItemDefaults.elevation(),
        val contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
        val interactionSource: MutableInteractionSource? = null,
        val content: @Composable () -> Unit,
    ) : SegmentedGroupItem()
}