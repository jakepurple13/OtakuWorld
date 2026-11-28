package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * List-detail navigator sized by the space Exceptions actually has. The default follows the
 * window, which on desktop/tablet can say "two panes" while this pane only fits one, and then
 * back and the detail arrow stop working.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberExceptionNavigator(twoPane: Boolean): ThreePaneScaffoldNavigator<Long> {
    val base = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
    return rememberListDetailPaneScaffoldNavigator<Long>(
        scaffoldDirective = base.copy(maxHorizontalPartitions = if (twoPane) 2 else 1),
    )
}

/**
 * Rows still sliding out when this leaves composition (back pressed, screen closed) are handed to
 * [commit], so a delete is never lost just because its animation didn't finish.
 */
@Composable
fun CommitExitingOnDispose(exiting: Set<Long>, commit: (Long) -> Unit) {
    val latest by rememberUpdatedState(exiting)
    val onCommit by rememberUpdatedState(commit)
    DisposableEffect(Unit) {
        onDispose { latest.forEach(onCommit) }
    }
}
