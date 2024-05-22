package com.programmersbox.mangaworld.reader.compose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.programmersbox.mangaworld.R
import com.programmersbox.uiviews.utils.components.OtakuScaffold


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun DrawerView(
    readVm: ReadViewModel,
    showToast: () -> Unit,
) {
    val drawerScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    OtakuScaffold(
        modifier = Modifier.nestedScroll(drawerScrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = drawerScrollBehavior,
                title = { Text(readVm.title) },
                actions = { PageIndicator(currentPage = readVm.list.size - readVm.currentChapter, pageCount = readVm.list.size) }
            )
        }
    ) { p ->
        LazyColumn(
            state = rememberLazyListState(
                readVm.currentChapter.coerceIn(
                    0,
                    readVm.list.lastIndex.coerceIn(minimumValue = 0, maximumValue = Int.MAX_VALUE)
                )
            ),
            contentPadding = p,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(readVm.list) { i, c ->

                var showChangeChapter by remember { mutableStateOf(false) }

                if (showChangeChapter) {
                    AlertDialog(
                        onDismissRequest = { showChangeChapter = false },
                        title = { Text(stringResource(R.string.changeToChapter, c.name)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showChangeChapter = false
                                    readVm.currentChapter = i
                                    readVm.addChapterToWatched(readVm.currentChapter, showToast)
                                }
                            ) { Text(stringResource(R.string.yes)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showChangeChapter = false }) { Text(stringResource(R.string.no)) }
                        }
                    )
                }

                WrapHeightNavigationDrawerItem(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .padding(horizontal = 4.dp),
                    label = { Text(c.name) },
                    selected = readVm.currentChapter == i,
                    onClick = { showChangeChapter = true },
                    shape = RoundedCornerShape(8.0.dp)
                )

                if (i < readVm.list.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun WrapHeightNavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = CircleShape,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    Surface(
        shape = shape,
        color = colors.containerColor(selected).value,
        modifier = modifier
            .heightIn(min = 56.dp)
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                interactionSource = interactionSource,
                role = Role.Tab,
                indication = null
            )
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                val iconColor = colors.iconColor(selected).value
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) {
                val labelColor = colors.textColor(selected).value
                CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                val badgeColor = colors.badgeColor(selected).value
                CompositionLocalProvider(LocalContentColor provides badgeColor, content = badge)
            }
        }
    }
}
