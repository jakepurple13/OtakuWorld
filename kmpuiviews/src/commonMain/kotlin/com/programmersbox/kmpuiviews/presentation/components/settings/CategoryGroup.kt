package com.programmersbox.kmpuiviews.presentation.components.settings

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@DslMarker
annotation class CategoryGroupMarker

@Composable
fun CategoryGroup(
    modifier: Modifier = Modifier,
    largeShape: CornerBasedShape = CategoryGroupDefaults.largeShape,
    smallShape: CornerBasedShape = CategoryGroupDefaults.smallShape,
    content: CategoryGroupScope.() -> Unit,
) {
    val categoryGroup = CategoryGroupImpl(rememberUpdatedState(content).value)
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
}

fun CategoryGroupScope.categorySetting(
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    title: @Composable () -> Unit,
) = category { CategorySetting(settingIcon = settingIcon, settingTitle = title) }

internal class CategoryGroupImpl(
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
}

internal sealed class CategoryGroupItem {
    data class Category(val content: @Composable () -> Unit) : CategoryGroupItem()
    data class Item(
        val includeDivider: Boolean,
        val content: @Composable () -> Unit,
    ) : CategoryGroupItem()
}