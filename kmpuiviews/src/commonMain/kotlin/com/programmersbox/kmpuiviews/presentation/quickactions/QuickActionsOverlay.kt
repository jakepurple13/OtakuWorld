package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.qa_check_failed
import otakuworld.kmpuiviews.generated.resources.qa_checking
import otakuworld.kmpuiviews.generated.resources.qa_checking_indeterminate
import otakuworld.kmpuiviews.generated.resources.qa_dismiss
import otakuworld.kmpuiviews.generated.resources.qa_new_updates
import otakuworld.kmpuiviews.generated.resources.qa_no_updates
import otakuworld.kmpuiviews.generated.resources.qa_sources_failed
import otakuworld.kmpuiviews.generated.resources.qa_view
import otakuworld.kmpuiviews.generated.resources.qa_waiting_to_start

private val CardShape = RoundedCornerShape(26.dp)

// Marks the progress snackbar; its text is read live from the state so progress updates don't
// replace (and re-animate) the snackbar. Dismissing it hides it until the check phase changes.
private object RunningVisuals : SnackbarVisuals {
    override val message: String = ""
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = true
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}

private data class ResultVisuals(
    override val message: String,
    override val actionLabel: String?,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration =
        if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
}

/**
 * Root overlay for quick actions: scrim, the card (grown from the bolt, or centered when opened
 * from the keyboard) and the update-check snackbar. Place it last inside the root
 * [SharedTransitionScope] so the card shares bounds with [QuickActionsBolt].
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SharedTransitionScope.QuickActionsOverlay() {
    val controller = koinInject<QuickActionsController>()
    val vm = koinViewModel<QuickActionsViewModel>()
    val open by controller.open.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val compact = LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact
    val cardWidth = if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(max = 480.dp).fillMaxWidth()

    // Composed only while open so it registers after the nav's own handler and wins over it;
    // an always-composed handler loses to NavDisplay once the back stack changes.
    if (open != null) BackHandler { controller.close() }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = open != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = controller::close,
                    )
            )
        }

        // No fade of its own: sharedBounds' enter/exit drive the crossfade with the bolt, so the
        // container stays solid while it grows
        AnimatedVisibility(
            visible = open == OpenOrigin.Bolt,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Horizontal system bars too: in landscape the nav bar sits at the side
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(8.dp)
                .then(cardWidth),
        ) {
            QuickActionsCard(
                state = uiState,
                onAction = vm::run,
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(QuickActionsSharedKey),
                    animatedVisibilityScope = this@AnimatedVisibility,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(250)),
                    boundsTransform = quickActionsBoundsTransform(opening = open != null),
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(CardShape),
                ),
            )
        }

        AnimatedVisibility(
            visible = open == OpenOrigin.Keyboard,
            enter = scaleIn(initialScale = 0.94f) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(8.dp)
                .then(cardWidth),
        ) {
            // Opened from the keyboard: focus the first row so the card is usable without a pointer
            val firstRow = remember { FocusRequester() }
            LaunchedEffect(Unit) { firstRow.requestFocus() }
            QuickActionsCard(state = uiState, onAction = vm::run, firstRowFocusRequester = firstRow)
        }

        QuickActionsSnackbar(
            state = uiState,
            onView = vm::openErrors,
            onResultDismissed = vm::dismissResult,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // The gesture bar is the floor when the app's nav bar is hidden
                .padding(
                    bottom = maxOf(
                        LocalNavHostPadding.current.calculateBottomPadding(),
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    )
                ),
        )
    }
}

@Composable
private fun AnimatedVisibilityScope.QuickActionsCard(
    state: QuickActionsUiState,
    onAction: (QuickAction) -> Unit,
    modifier: Modifier = Modifier,
    firstRowFocusRequester: FocusRequester? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = CardShape,
        modifier = modifier,
    ) {
        // Rows appear once the card is mostly open, and leave before it shrinks away
        QuickActionsList(
            state = state,
            onAction = onAction,
            firstRowFocusRequester = firstRowFocusRequester,
            modifier = Modifier
                .animateEnterExit(
                    enter = fadeIn(tween(delayMillis = 150)),
                    exit = fadeOut(tween(100)),
                )
                // Keeps the last row reachable at large font scales in landscape
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
        )
    }
}

@Composable
private fun QuickActionsSnackbar(
    state: QuickActionsUiState,
    onView: () -> Unit,
    onResultDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hostState = remember { SnackbarHostState() }
    val checkState = state.checkState
    val userRequested = state.userRequested

    // Keyed on the phase, not the state: progress ticks update the running snackbar's text in place.
    val phase: Any = when (checkState) {
        is ManualCheckState.Finished -> checkState
        else -> checkState::class
    }
    LaunchedEffect(phase, userRequested) {
        when (checkState) {
            ManualCheckState.Idle -> hostState.currentSnackbarData?.dismiss()
            // Scheduled checks spin the bolt but never raise the snackbar
            is ManualCheckState.Running -> if (userRequested) hostState.showSnackbar(RunningVisuals)
            is ManualCheckState.Finished -> {
                val visuals = when (val outcome = checkState.outcome) {
                    is CheckOutcome.Updates -> ResultVisuals(getString(Res.string.qa_new_updates, outcome.count), null)
                    CheckOutcome.NoUpdates -> ResultVisuals(getString(Res.string.qa_no_updates), null)
                    CheckOutcome.SourcesFailed -> ResultVisuals(
                        getString(Res.string.qa_sources_failed),
                        getString(Res.string.qa_view),
                    )
                    // No report was written, so there is nothing to view
                    CheckOutcome.CheckFailed -> ResultVisuals(getString(Res.string.qa_check_failed), null)
                }
                if (hostState.showSnackbar(visuals) == SnackbarResult.ActionPerformed) onView()
                onResultDismissed()
            }
        }
    }

    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        if (data.visuals is RunningVisuals) {
            val running = checkState as? ManualCheckState.Running
            val checked = running?.checked
            val total = running?.total
            Snackbar(
                modifier = Modifier.padding(12.dp),
                dismissAction = {
                    IconButton(onClick = data::dismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.qa_dismiss))
                    }
                },
            ) {
                Text(
                    when {
                        running?.waitingToStart == true -> stringResource(Res.string.qa_waiting_to_start)
                        checked != null && total != null && total > 0 ->
                            stringResource(Res.string.qa_checking, checked, total)

                        else -> stringResource(Res.string.qa_checking_indeterminate)
                    }
                )
            }
        } else {
            Snackbar(data, modifier = Modifier.padding(12.dp))
        }
    }
}
