package com.programmersbox.mangaworld.reader.compose

import android.text.format.DateFormat
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.programmersbox.helpfulutils.battery
import com.programmersbox.helpfulutils.timeTick
import com.programmersbox.mangasettings.PlayingMiddleAction
import com.programmersbox.mangasettings.PlayingStartAction
import com.programmersbox.mangaworld.R
import com.programmersbox.uiviews.utils.BatteryInformation
import com.programmersbox.uiviews.utils.LocalNavController
import kotlinx.coroutines.flow.launchIn

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@Composable
internal fun ReaderTopBar(
    pages: List<String>,
    currentPage: Int,
    currentChapter: String,
    playingStartAction: PlayingStartAction,
    playingMiddleAction: PlayingMiddleAction,
    showBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        windowInsets = WindowInsets(0.dp),
        modifier = modifier,
        navigationIcon = {
            Crossfade(
                targetState = playingStartAction,
                label = "startAction"
            ) { target ->
                when (target) {
                    PlayingStartAction.Battery -> {
                        val context = LocalContext.current
                        var batteryColor by remember { mutableStateOf(Color.White) }
                        var batteryIcon by remember { mutableStateOf(BatteryInformation.BatteryViewType.UNKNOWN) }
                        var batteryPercent by remember { mutableFloatStateOf(0f) }
                        val batteryInformation = remember(context) { BatteryInformation(context) }

                        LaunchedEffect(context) {
                            batteryInformation.composeSetupFlow(
                                Color.White
                            ) {
                                batteryColor = it.first
                                batteryIcon = it.second
                            }
                                .launchIn(this)
                        }

                        DisposableEffect(context) {
                            val batteryInfo = context.battery {
                                batteryPercent = it.percent
                                batteryInformation.batteryLevel.tryEmit(it.percent)
                                batteryInformation.batteryInfo.tryEmit(it)
                            }
                            onDispose { context.unregisterReceiver(batteryInfo) }
                        }
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                batteryIcon.composeIcon,
                                contentDescription = null,
                                tint = animateColorAsState(
                                    if (batteryColor == Color.White) MaterialTheme.colorScheme.onSurface
                                    else batteryColor, label = ""
                                ).value
                            )
                            Text(
                                "${batteryPercent.toInt()}%",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    PlayingStartAction.CurrentChapter -> {
                        Text(
                            currentChapter,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    PlayingStartAction.None -> {}
                    PlayingStartAction.UNRECOGNIZED -> {}
                }
            }
        },
        title = {
            Crossfade(
                targetState = playingMiddleAction,
                label = "middleAction"
            ) { target ->
                when (target) {
                    PlayingMiddleAction.Time -> {
                        var time by remember { mutableLongStateOf(System.currentTimeMillis()) }

                        val activity = LocalActivity.current

                        DisposableEffect(LocalContext.current) {
                            val timeReceiver = activity?.timeTick { _, _ -> time = System.currentTimeMillis() }
                            onDispose { activity?.unregisterReceiver(timeReceiver) }
                        }

                        Text(
                            DateFormat.getTimeFormat(LocalContext.current).format(time).toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(4.dp)
                        )
                    }

                    PlayingMiddleAction.Nothing -> {}
                    PlayingMiddleAction.UNRECOGNIZED -> {}
                }
            }
        },
        actions = {
            PageIndicator(
                currentPage = currentPage + 1,
                pageCount = pages.size,
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.CenterVertically)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = if (showBlur) Color.Transparent else Color.Unspecified)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FloatingBottomBar(
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
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalFloatingToolbar(
            modifier = modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .offset(y = -FloatingToolbarDefaults.ScreenOffset),
            expanded = showFloatBar,
            leadingContent = {
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
            },
            trailingContent = {
                IconButton(
                    onClick = onSettingsClick,
                    //modifier = Modifier.weight(1f)
                ) { Icon(Icons.Default.Settings, null) }
            },
            scrollBehavior = exitAlwaysScrollBehavior,
            content = {
                FilledIconButton(
                    onClick = { onShowFloatBarChange(!showFloatBar) },
                    modifier = Modifier
                        .width(64.dp)
                ) {
                    Icon(
                        if (showFloatBar) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = "Localized description"
                    )
                }
            },
        )
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