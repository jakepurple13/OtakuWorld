package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.quickactions.isMacOs
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.tabs_refresh
import otakuworld.kmpuiviews.generated.resources.tabs_refresh_tooltip
import otakuworld.kmpuiviews.generated.resources.tabs_refresh_up_to_date
import otakuworld.kmpuiviews.generated.resources.tabs_stop_loading
import otakuworld.kmpuiviews.generated.resources.tabs_stop_loading_tooltip

/**
 * The toolbar's Refresh button, at the end of the path pill. Turns into Stop while a page that can cancel is loading;
 * disabled on pages with nothing to reload and while a page that can't cancel is loading.
 */
/** How long after a Refresh click a Stop click is ignored, as browsers guard their reload button. */
private const val StopGuardMillis = 500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RefreshButton(
    registry: TabRefreshRegistry,
    reduceMotion: Boolean,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    now: () -> Long = System::currentTimeMillis,
) {
    val canStop = registry.canStop
    val turn = remember { Animatable(0f) }
    // When the last Refresh click landed, so the second click of a double-click can't stop the reload it started.
    var refreshedAt by remember { mutableLongStateOf(Long.MIN_VALUE / 2) }
    val scope = rememberCoroutineScope()

    val tooltip = when {
        canStop -> stringResource(Res.string.tabs_stop_loading_tooltip)
        !registry.canRefresh -> stringResource(Res.string.tabs_refresh_up_to_date)
        else -> stringResource(Res.string.tabs_refresh_tooltip, if (isMacOs) "⌘R" else "Ctrl+R")
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        IconButton(
            onClick = {
                if (canStop) {
                    if (now() - refreshedAt >= StopGuardMillis) onStop()
                } else {
                    refreshedAt = now()
                    if (!reduceMotion) scope.launch {
                        turn.snapTo(0f)
                        turn.animateTo(360f, tween(450, easing = EmphasizedDecelerate))
                    }
                    onRefresh()
                }
            },
            enabled = canStop || (registry.canRefresh && !registry.refreshing),
            modifier = Modifier.size(30.dp),
        ) {
            AnimatedContent(
                targetState = canStop,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
                    }
                },
                label = "refresh-stop",
            ) { stop ->
                if (stop) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.tabs_stop_loading),
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.tabs_refresh),
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = turn.value },
                    )
                }
            }
        }
    }
}
