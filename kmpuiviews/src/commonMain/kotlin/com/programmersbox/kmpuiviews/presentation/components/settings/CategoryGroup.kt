package com.programmersbox.kmpuiviews.presentation.components.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
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

@DslMarker
annotation class CategoryGroupMarker

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryGroup(
    modifier: Modifier = Modifier,
    largeShape: CornerBasedShape = CategoryGroupDefaults.largeShape,
    smallShape: CornerBasedShape = CategoryGroupDefaults.smallShape,
    content: CategoryGroupScope.() -> Unit,
) {
    val updatedContent by rememberUpdatedState(content)
    val categoryGroup = remember(updatedContent) {
        CategoryGroupImpl(updatedContent)
    }
    val stateHolder = rememberSaveableStateHolder()

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        for (i in 0 until categoryGroup.size) {
            ElevatedCard(
                shape = when {
                    categoryGroup.size == 1 -> largeShape

                    i == 0 -> largeShape.copy(
                        bottomEnd = smallShape.bottomEnd,
                        bottomStart = smallShape.bottomStart
                    )

                    i == categoryGroup.size - 1 -> largeShape.copy(
                        topEnd = smallShape.topEnd,
                        topStart = smallShape.topStart
                    )

                    else -> smallShape
                },
            ) {
                stateHolder.SaveableStateProvider(i) {
                    when (val item = categoryGroup.get(i)) {
                        is CategoryGroupItem.Category -> {
                            item.content()
                            if (i != categoryGroup.size - 1) {
                                CategoryGroupDefaults.Divider()
                            }
                        }

                        is CategoryGroupItem.Item -> {
                            item.content()
                            if (i != categoryGroup.size - 1 && item.includeDivider) {
                                CategoryGroupDefaults.Divider()
                            }
                        }

                        is CategoryGroupItem.SegmentedListItem -> item.content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryGroupListItem(
    modifier: Modifier = Modifier,
    content: CategoryGroupScope.() -> Unit,
) {
    val updatedContent by rememberUpdatedState(content)
    val categoryGroup = remember(updatedContent) {
        CategoryGroupImpl(updatedContent)
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
                    is CategoryGroupItem.Category -> {
                        item.content()
                        if (i != categoryGroup.size - 1) {
                            CategoryGroupDefaults.Divider()
                        }
                    }

                    is CategoryGroupItem.Item -> {
                        item.content()
                        if (i != categoryGroup.size - 1 && item.includeDivider) {
                            CategoryGroupDefaults.Divider()
                        }
                    }

                    is CategoryGroupItem.SegmentedListItem -> {
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
                }
            }
        }
    }
}

object CategoryGroupDefaults {
    @Composable
    fun Divider() = HorizontalDivider(
        color = MaterialTheme.colorScheme.surface,
        thickness = 2.dp
    )

    val largeShape: CornerBasedShape
        @Composable
        get() = MaterialTheme.shapes.extraLarge

    val smallShape: CornerBasedShape
        @Composable
        get() = MaterialTheme.shapes.extraSmall
}


@CategoryGroupMarker
interface CategoryGroupScope {
    fun category(content: @Composable () -> Unit)

    fun item(
        includeDivider: Boolean = true,
        content: @Composable () -> Unit,
    )

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
}

fun CategoryGroupScope.categorySetting(
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    title: @Composable () -> Unit,
) = category { CategorySetting(settingIcon = settingIcon, settingTitle = title) }

private class CategoryGroupImpl(
    content: CategoryGroupScope.() -> Unit = {},
) : CategoryGroupScope {
    val intervals: MutableIntervalList<CategoryGroupItem> = MutableIntervalList()

    val size: Int get() = intervals.size

    fun get(index: Int): CategoryGroupItem = intervals[index].value

    init {
        apply(content)
    }

    override fun category(content: @Composable (() -> Unit)) {
        intervals.addInterval(1, CategoryGroupItem.Category(content))
    }

    override fun item(
        includeDivider: Boolean,
        content: @Composable () -> Unit,
    ) {
        intervals.addInterval(
            1,
            CategoryGroupItem.Item(
                includeDivider = includeDivider,
                content = content
            )
        )
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
            CategoryGroupItem.SegmentedListItem(
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
}

private sealed class CategoryGroupItem {
    data class Category(val content: @Composable () -> Unit) : CategoryGroupItem()
    data class Item(
        val includeDivider: Boolean,
        val content: @Composable () -> Unit,
    ) : CategoryGroupItem()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    data class SegmentedListItem(
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
    ) : CategoryGroupItem()
}
