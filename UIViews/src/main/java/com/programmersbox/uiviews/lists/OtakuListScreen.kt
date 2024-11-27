@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.programmersbox.uiviews.lists

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.adaptive.layout.AnimatedPaneScope
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.occludingVerticalHingeBounds
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.uiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LocalSettingsHandling
import com.programmersbox.uiviews.utils.biometricPrompting
import com.programmersbox.uiviews.utils.findActivity
import com.programmersbox.uiviews.utils.rememberBiometricPrompt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun OtakuListScreen(
    listDao: ListDao = LocalCustomListDao.current,
    viewModel: OtakuListViewModel = viewModel { OtakuListViewModel(listDao) },
    customListViewModel: OtakuCustomListViewModel = viewModel { OtakuCustomListViewModel(listDao) },
    isHorizontal: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showListDetail by LocalSettingsHandling.current.rememberShowListDetail()

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

    val details: @Composable ThreePaneScaffoldPaneScope.() -> Unit = {
        AnimatedPane {
            AnimatedContent(
                targetState = customListViewModel.customItem,
                label = "",
                transitionSpec = {
                    if (initialState != null && targetState != null)
                        EnterTransition.None togetherWith ExitTransition.None
                    else
                        (slideInHorizontally { -it } + fadeIn()) togetherWith (fadeOut() + slideOutHorizontally { -it })
                }
            ) { targetState ->
                if (targetState != null) {
                    OtakuCustomListScreen(
                        viewModel = customListViewModel,
                        customItem = targetState,
                        writeToFile = customListViewModel::writeToFile,
                        isHorizontal = isHorizontal,
                        deleteAll = customListViewModel::deleteAll,
                        rename = customListViewModel::rename,
                        searchQuery = customListViewModel.searchQuery,
                        setQuery = customListViewModel::setQuery,
                        searchBarActive = customListViewModel.searchBarActive,
                        onSearchBarActiveChange = { customListViewModel.searchBarActive = it },
                        navigateBack = {
                            customListViewModel.setList(null)
                            scope.launch { state.navigateBack() }
                        },
                        addSecurityItem = {
                            scope.launch { listDao.updateBiometric(it, true) }
                        },
                        removeSecurityItem = {
                            scope.launch { listDao.updateBiometric(it, false) }
                        },
                    )
                    BackHandler {
                        customListViewModel.setList(null)
                        scope.launch { state.navigateBack() }
                    }
                } else {
                    NoDetailSelected()
                }
            }
        }
    }

    val navigate = {
        scope.launch {
            if (showListDetail)
                state.navigateTo(ListDetailPaneScaffoldRole.Detail)
            else
                state.navigateTo(ListDetailPaneScaffoldRole.Extra)
        }
    }

    var temp by remember { mutableStateOf<CustomList?>(null) }

    val biometricPrompt = rememberBiometricPrompt(
        onAuthenticationSucceeded = {
            customListViewModel.setList(temp)
            temp = null
            navigate()
        },
        onAuthenticationFailed = {
            customListViewModel.setList(null)
            temp = null
        }
    )

    ListDetailPaneScaffold(
        directive = state.scaffoldDirective,
        value = state.scaffoldValue,
        listPane = {
            AnimatedPane {
                OtakuListView(
                    customItem = customListViewModel.customItem,
                    customLists = viewModel.customLists,
                    navigateDetail = {
                        if (it.item.useBiometric) {
                            temp = it
                            biometricPrompting(
                                context.findActivity(),
                                biometricPrompt
                            ).authenticate(
                                BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Authentication required")
                                    .setSubtitle("In order to view ${it.item.name}, please authenticate")
                                    .setNegativeButtonText("Never Mind")
                                    .build()
                            )
                        } else {
                            customListViewModel.setList(it)
                            temp = null
                            navigate()
                        }
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
fun ThreePaneScaffoldPaneScope.AnimatedPanes(
    modifier: Modifier,
    content: @Composable AnimatedPaneScope.() -> Unit,
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
        verticalPartitionSpacerSize = verticalSpacerSize,
        horizontalPartitionSpacerSize = horizontalSpacerSize,
        defaultPanePreferredWidth = 360.dp,
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