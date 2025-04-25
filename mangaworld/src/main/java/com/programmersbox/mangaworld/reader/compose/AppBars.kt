package com.programmersbox.mangaworld.reader.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.mangaworld.R

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@Composable
fun ReaderTopBar(
    currentChapter: String,
    onSettingsClick: () -> Unit,
    showBlur: Boolean,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        windowInsets = windowInsets,
        modifier = modifier,
        navigationIcon = { BackButton() },
        title = {
            Text(
                currentChapter,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.basicMarquee()
            )
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
            ) { Icon(Icons.Default.Settings, null) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = if (showBlur) Color.Transparent else Color.Unspecified)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FloatingFloatingActionButton(
    vm: ReadViewModel,
    onPageSelectClick: () -> Unit,
    onSettingsClick: () -> Unit,
    chapterChange: () -> Unit,
    onChapterShow: () -> Unit,
    modifier: Modifier = Modifier,
    showFloatBar: Boolean = true,
    onShowFloatBarChange: (Boolean) -> Unit = {},
    exitAlwaysScrollBehavior: FloatingToolbarScrollBehavior? = null,
) {
    //TODO: Maybe have a setting to choose between vertical and horizontal?
    VerticalFloatingToolbar(
        expanded = showFloatBar,
        scrollBehavior = exitAlwaysScrollBehavior,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(
                onClick = { onShowFloatBarChange(!showFloatBar) },
            ) {
                Icon(
                    if (showFloatBar) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = "Localized description"
                )
            }
        },
        modifier = modifier
    ) {
        val prevShown = vm.currentChapter < vm.list.lastIndex
        val nextShown = vm.currentChapter > 0

        AnimatedVisibility(
            visible = prevShown && vm.list.size > 1,
            enter = expandHorizontally(expandFrom = Alignment.Start),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            PreviousIconButton(
                previousChapter = chapterChange,
                vm = vm,
            )
        }

        GoBackIconButton()

        AnimatedVisibility(
            visible = nextShown && vm.list.size > 1,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            NextIconButton(
                nextChapter = chapterChange,
                vm = vm,
            )
        }

        IconButton(
            onClick = onPageSelectClick,
        ) { Icon(Icons.Default.GridOn, null) }

        IconButton(
            onClick = onChapterShow,
        ) { Icon(Icons.Default.Numbers, null) }

        IconButton(
            onClick = onSettingsClick,
        ) { Icon(Icons.Default.Settings, null) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BottomBar(
    vm: ReadViewModel,
    onPageSelectClick: () -> Unit,
    onSettingsClick: () -> Unit,
    chapterChange: () -> Unit,
    onChapterShow: () -> Unit,
    showBlur: Boolean,
    isAmoledMode: Boolean,
    modifier: Modifier = Modifier,
) {
    BottomAppBar(
        modifier = modifier,
        windowInsets = WindowInsets(0.dp),
        containerColor = when {
            showBlur -> Color.Transparent
            isAmoledMode -> MaterialTheme.colorScheme.surface
            else -> BottomAppBarDefaults.containerColor
        }
    ) {
        val prevShown = vm.currentChapter < vm.list.lastIndex
        val nextShown = vm.currentChapter > 0

        AnimatedVisibility(
            visible = prevShown && vm.list.size > 1,
            enter = expandHorizontally(expandFrom = Alignment.Start),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            PreviousButton(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .weight(
                        when {
                            prevShown && nextShown -> 8f / 3f
                            prevShown -> 4f
                            else -> 4f
                        }
                    ),
                previousChapter = chapterChange,
                vm = vm
            )
        }

        GoBackButton(
            modifier = Modifier
                .weight(
                    animateFloatAsState(
                        when {
                            prevShown && nextShown -> 8f / 3f
                            prevShown || nextShown -> 4f
                            else -> 8f
                        }, label = ""
                    ).value
                )
        )

        AnimatedVisibility(
            visible = nextShown && vm.list.size > 1,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            NextButton(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .weight(
                        when {
                            prevShown && nextShown -> 8f / 3f
                            nextShown -> 4f
                            else -> 4f
                        }
                    ),
                nextChapter = chapterChange,
                vm = vm
            )
        }
        //The three buttons above will equal 8f
        //So these two need to add up to 2f
        IconButton(
            onClick = onPageSelectClick,
            modifier = Modifier.weight(1f)
        ) { Icon(Icons.Default.GridOn, null) }

        IconButton(
            onClick = onChapterShow,
            modifier = Modifier.weight(1f)
        ) { Icon(Icons.Default.Numbers, null) }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.weight(1f)
        ) { Icon(Icons.Default.Settings, null) }
    }
}

@Composable
private fun GoBackButton(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    OutlinedButton(
        onClick = { navController.popBackStack() },
        modifier = modifier,
        border = BorderStroke(ButtonDefaults.outlinedButtonBorder(true).width, MaterialTheme.colorScheme.primary)
    ) { Text(stringResource(id = R.string.goBack), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun NextButton(
    vm: ReadViewModel,
    modifier: Modifier = Modifier,
    nextChapter: () -> Unit,
) {
    Button(
        onClick = { vm.addChapterToWatched(--vm.currentChapter, nextChapter) },
        modifier = modifier
    ) { Text(stringResource(id = R.string.loadNextChapter)) }
}

@Composable
private fun PreviousButton(
    vm: ReadViewModel,
    modifier: Modifier = Modifier,
    previousChapter: () -> Unit,
) {
    TextButton(
        onClick = { vm.addChapterToWatched(++vm.currentChapter, previousChapter) },
        modifier = modifier
    ) { Text(stringResource(id = R.string.loadPreviousChapter)) }
}

@Composable
private fun PreviousIconButton(
    vm: ReadViewModel,
    modifier: Modifier = Modifier,
    previousChapter: () -> Unit,
) {
    IconButton(
        onClick = { vm.addChapterToWatched(++vm.currentChapter, previousChapter) },
        modifier = modifier
    ) { Icon(Icons.Default.ArrowBack, null) }
}

@Composable
private fun GoBackIconButton(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    OutlinedIconButton(
        onClick = { navController.popBackStack() },
        modifier = modifier,
        border = BorderStroke(ButtonDefaults.outlinedButtonBorder(true).width, MaterialTheme.colorScheme.primary)
    ) { Icon(Icons.Default.Home, null) }
}

@Composable
private fun NextIconButton(
    vm: ReadViewModel,
    modifier: Modifier = Modifier,
    nextChapter: () -> Unit,
) {
    FilledIconButton(
        onClick = { vm.addChapterToWatched(--vm.currentChapter, nextChapter) },
        modifier = modifier
    ) { Icon(Icons.Default.ArrowForward, null) }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FloatingBottomBar(
    onPageSelectClick: () -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onChapterShow: () -> Unit,
    chapterNumber: String,
    chapterCount: String,
    currentPage: Int,
    pages: Int,
    showBlur: Boolean,
    isAmoledMode: Boolean,
    previousButtonEnabled: Boolean,
    nextButtonEnabled: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = when {
        showBlur -> Color.Transparent
        isAmoledMode -> MaterialTheme.colorScheme.surface
        else -> NavigationBarDefaults.containerColor
    },
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
) {
    FloatingNavigationBar(
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onPreviousChapter,
            enabled = previousButtonEnabled,
            icon = { Icon(Icons.Default.ArrowCircleLeft, null) },
            label = { Text(stringResource(id = R.string.loadPreviousChapter)) }
        )

        NavigationBarItem(
            selected = false,
            onClick = onNextChapter,
            enabled = nextButtonEnabled,
            icon = { Icon(Icons.Default.ArrowCircleRight, null) },
            label = { Text(stringResource(id = R.string.loadNextChapter)) }
        )

        NavigationBarItem(
            selected = false,
            onClick = onChapterShow,
            icon = { Text("#$chapterNumber/$chapterCount") },
            label = { Text("Chapters") }
        )

        NavigationBarItem(
            selected = false,
            onClick = onPageSelectClick,
            icon = {
                PageIndicator(
                    currentPage = currentPage + 1,
                    pageCount = pages,
                    modifier = Modifier
                )
            },
            label = { Text("Pages") }
        )
    }
}

@Composable
private fun FloatingNavigationBar(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shape = shape,
        border = BorderStroke(
            width = 0.5.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
            ),
        ),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(80.dp)
                .selectableGroup(),
            content = content,
        )
    }
}