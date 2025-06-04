package com.programmersbox.uiviews.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.ScreenBottomItem
import com.programmersbox.kmpuiviews.presentation.components.item
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberMultipleBarState(
    hideOnClick: Boolean = true,
) = remember(hideOnClick) { MultipleBarState(hideOnClick) }

class MultipleBarState(
    hideOnClick: Boolean = true,
) {
    var expanded by mutableStateOf(false)
        private set
    var showHorizontalBar by mutableStateOf(false)
        private set

    var hideOnClick by mutableStateOf(hideOnClick)

    suspend fun show() {
        expanded = false
        showHorizontalBar = true
        delay(250)
        expanded = true
    }

    suspend fun hide() {
        expanded = false
        delay(250)
        showHorizontalBar = false
    }
}

@Composable
fun BoxScope.MultipleActions(
    state: MultipleBarState,
    middleNavItem: com.programmersbox.datastore.MiddleNavigationAction,
    multipleActions: com.programmersbox.datastore.MiddleMultipleActions,
    navigationActions: NavigationActions,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    if (middleNavItem == com.programmersbox.datastore.MiddleNavigationAction.Multiple) {
        MultipleActions(
            state = state,
            leadingContent = {
                multipleActions.startAction.item?.ScreenBottomItem(
                    onClick = {
                        scope.launch { if (state.hideOnClick) state.hide() }
                        navigationActions.homeScreenNavigate(it.screen)
                    }
                )
            },
            trailingContent = {
                multipleActions.endAction.item?.ScreenBottomItem(
                    onClick = {
                        scope.launch { if (state.hideOnClick) state.hide() }
                        navigationActions.homeScreenNavigate(it.screen)
                    }
                )
            },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.MultipleActions(
    state: MultipleBarState,
    leadingContent: @Composable RowScope.() -> Unit,
    trailingContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = state.showHorizontalBar,
        enter = slideInVertically(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        ) { it / 2 } + fadeIn(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        ),
        exit = slideOutVertically(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        ) { it / 2 } + fadeOut(
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        ),
        modifier = modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .offset(y = -FloatingToolbarDefaults.ScreenOffset),
    ) {
        HorizontalFloatingToolbar(
            expanded = state.expanded,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
        ) {
            FilledIconButton(
                modifier = Modifier.width(64.dp),
                onClick = { scope.launch { state.hide() } }
            ) {
                Icon(
                    if (state.expanded) Icons.Default.UnfoldLess else Icons.Filled.UnfoldMore,
                    contentDescription = "Localized description"
                )
            }
        }
    }
}