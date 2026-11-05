package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.LocalSharedElementScope
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.quick_actions

const val QuickActionsSharedKey = "quick-actions-card"

private val EmphasizedDecelerate = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** Open grows with emphasized decelerate; close shrinks back with emphasized accelerate. */
internal fun quickActionsBoundsTransform(opening: Boolean) = BoundsTransform { _, _ ->
    if (opening) tween(durationMillis = 400, easing = EmphasizedDecelerate)
    else tween(durationMillis = 300, easing = EmphasizedAccelerate)
}

/**
 * Top bar button that opens the quick actions card. The 48dp slot stays put while the circle
 * hides, so the rest of the action row never shifts.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QuickActionsBolt(modifier: Modifier = Modifier) {
    val controller = koinInject<QuickActionsController>()
    val vm = koinViewModel<QuickActionsViewModel>()
    val open by controller.open.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val sharedScope = LocalSharedElementScope.current
    val description = stringResource(Res.string.quick_actions)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(48.dp),
    ) {
        // No fade of its own: sharedBounds' enter/exit drive the crossfade with the card
        AnimatedVisibility(
            visible = open != OpenOrigin.Bolt,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            val shared = sharedScope?.run {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(QuickActionsSharedKey),
                    animatedVisibilityScope = this@AnimatedVisibility,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(150)),
                    boundsTransform = quickActionsBoundsTransform(opening = open != null),
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(CircleShape),
                )
            } ?: Modifier
            Box(
                contentAlignment = Alignment.Center,
                modifier = shared
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClickLabel = description, role = Role.Button) {
                        controller.toggle(OpenOrigin.Bolt)
                    }
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = description,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (uiState.checkState is ManualCheckState.Running) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}
