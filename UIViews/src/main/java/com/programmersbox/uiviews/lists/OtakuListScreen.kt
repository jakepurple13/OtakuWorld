@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.programmersbox.uiviews.lists

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.allVerticalHingeBounds
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.occludingVerticalHingeBounds
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.uiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalSettingsHandling

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun OtakuListScreen(
    listDao: ListDao = LocalCustomListDao.current,
    viewModel: OtakuListViewModel = viewModel { OtakuListViewModel(listDao) },
    isHorizontal: Boolean = false,
) {
    val showListDetail by LocalSettingsHandling.current
        .showListDetail
        .collectAsStateWithLifecycle(true)

    val windowSize = with(LocalDensity.current) {
        currentWindowSize().toSize().toDpSize()
    }
    val windowSizeClass = remember(windowSize) { WindowSizeClass.calculateFromSize(windowSize) }

    val state = rememberListDetailPaneScaffoldNavigator<Int>(
        scaffoldDirective = calculateStandardPaneScaffoldDirective(
            currentWindowAdaptiveInfo(),
            windowSizeClass = windowSizeClass
        )
    )

    val details: @Composable ThreePaneScaffoldScope.() -> Unit = {
        AnimatedPanes(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = viewModel.customItem,
                label = "",
                transitionSpec = {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (fadeOut() + slideOutHorizontally { -it })
                }
            ) { targetState ->
                if (targetState != null) {
                    OtakuCustomListScreen(
                        customItem = targetState,
                        writeToFile = viewModel::writeToFile,
                        isHorizontal = isHorizontal,
                        deleteAll = viewModel::deleteAll,
                        rename = viewModel::rename,
                        listBySource = viewModel.listBySource,
                        removeItems = viewModel::removeItems,
                        items = viewModel.items,
                        searchItems = viewModel.searchItems,
                        searchQuery = viewModel.searchQuery,
                        setQuery = viewModel::setQuery,
                        searchBarActive = viewModel.searchBarActive,
                        onSearchBarActiveChange = { viewModel.searchBarActive = it },
                        navigateBack = {
                            viewModel.customItem = null
                            state.navigateBack()
                        },
                    )
                    BackHandler {
                        viewModel.customItem = null
                        state.navigateBack()
                    }
                } else {
                    NoDetailSelected()
                }
            }
        }
    }

    ListDetailPaneScaffold(
        directive = state.scaffoldDirective,
        value = state.scaffoldValue,
        windowInsets = WindowInsets(0.dp),
        listPane = {
            AnimatedPanes(modifier = Modifier.fillMaxSize()) {
                OtakuListView(
                    customItem = viewModel.customItem,
                    customLists = viewModel.customLists,
                    navigateDetail = {
                        viewModel.customItem = it
                        if (showListDetail)
                            state.navigateTo(ListDetailPaneScaffoldRole.Detail)
                        else
                            state.navigateTo(ListDetailPaneScaffoldRole.Extra)
                    }
                )
            }
        },
        detailPane = { if (showListDetail) details() },
        extraPane = if (!showListDetail) {
            { details() }
        } else null
    )
}

@ExperimentalMaterial3AdaptiveApi
@Composable
fun ThreePaneScaffoldScope.AnimatedPanes(
    modifier: Modifier,
    content: (@Composable ThreePaneScaffoldScope.() -> Unit),
) {
    AnimatedPane(
        modifier = modifier,
        content = content
    )
}

@ExperimentalMaterial3AdaptiveApi
fun calculateStandardPaneScaffoldDirective(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    windowSizeClass: WindowSizeClass,
    verticalHingePolicy: HingePolicy = HingePolicy.AvoidSeparating,
): PaneScaffoldDirective {
    //return androidx.compose.material3.adaptive.layout.calculateStandardPaneScaffoldDirective(windowAdaptiveInfo, verticalHingePolicy)
    val maxHorizontalPartitions: Int
    val contentPadding: PaddingValues
    val verticalSpacerSize: Dp// = 0.dp
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            maxHorizontalPartitions = 1
            contentPadding = PaddingValues(0.dp)
            verticalSpacerSize = 0.dp
        }

        WindowWidthSizeClass.Medium -> {
            maxHorizontalPartitions = 1
            contentPadding = PaddingValues(horizontal = 0.dp)
            verticalSpacerSize = 0.dp
        }

        else -> {
            maxHorizontalPartitions = 2
            contentPadding = PaddingValues(horizontal = 0.dp)
            verticalSpacerSize = 24.dp
        }
    }
    val maxVerticalPartitions: Int
    val horizontalSpacerSize: Dp = 0.dp

    // TODO(conradchen): Confirm the table top mode settings
    if (windowAdaptiveInfo.windowPosture.isTabletop) {
        maxVerticalPartitions = 2
        //horizontalSpacerSize = 24.dp
    } else {
        maxVerticalPartitions = 1
        //horizontalSpacerSize = 0.dp
    }

    val posture = windowAdaptiveInfo.windowPosture

    return PaneScaffoldDirective(
        maxHorizontalPartitions = maxHorizontalPartitions,
        contentPadding = contentPadding,
        verticalPartitionSpacerSize = verticalSpacerSize,
        horizontalPartitionSpacerSize = horizontalSpacerSize,
        maxVerticalPartitions = maxVerticalPartitions,
        excludedBounds = when (verticalHingePolicy) {
            HingePolicy.AvoidSeparating -> posture.separatingVerticalHingeBounds
            HingePolicy.AvoidOccluding -> posture.occludingVerticalHingeBounds
            HingePolicy.AlwaysAvoid -> posture.allVerticalHingeBounds
            else -> emptyList()
        }
    )
}

@Composable
private fun NoDetailSelected() {
    Surface {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
                Text("Select a list to view!")
            }
        }
    }
}