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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import com.programmersbox.sharedcomponents.notifier.ProgressHandle
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.qa_check_failed
import otakuworld.kmpuiviews.generated.resources.qa_checking
import otakuworld.kmpuiviews.generated.resources.qa_checking_indeterminate
import otakuworld.kmpuiviews.generated.resources.qa_new_updates
import otakuworld.kmpuiviews.generated.resources.qa_no_updates
import otakuworld.kmpuiviews.generated.resources.qa_sources_failed
import otakuworld.kmpuiviews.generated.resources.qa_view
import otakuworld.kmpuiviews.generated.resources.qa_waiting_to_start

private val CardShape = RoundedCornerShape(26.dp)

/**
 * Root overlay for quick actions: scrim, the card (grown from the bolt, or centered when opened
 * from the keyboard) and sends update-check banners. Place it last inside the root
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

        QuickActionsBanners(
            state = uiState,
            onView = vm::openErrors,
            onResultDismissed = vm::dismissResult,
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
private fun QuickActionsBanners(
    state: QuickActionsUiState,
    onView: () -> Unit,
    onResultDismissed: () -> Unit,
) {
    val notifier = koinInject<InAppNotifier>()
    val checkState = state.checkState
    val userRequested = state.userRequested
    var handle by remember { mutableStateOf<ProgressHandle?>(null) }
    val currentHandle by rememberUpdatedState(handle)
    DisposableEffect(Unit) { onDispose { currentHandle?.dismiss() } }

    // Keyed on the phase, not the state: progress ticks update the running banner in place.
    val phase: Any = when (checkState) {
        is ManualCheckState.Finished -> checkState
        else -> checkState::class
    }
    LaunchedEffect(phase, userRequested) {
        when (checkState) {
            ManualCheckState.Idle -> {
                handle?.dismiss()
                handle = null
            }
            // Scheduled checks spin the bolt but never raise a banner
            is ManualCheckState.Running -> if (userRequested && handle == null) {
                handle = notifier.progress(getString(Res.string.qa_checking_indeterminate))
            }
            is ManualCheckState.Finished -> {
                val onViewAction: suspend () -> Unit = { onView() }
                // Resolve strings before taking the handle: a cancellation in between would orphan it
                val outcome = checkState.outcome
                val title = when (outcome) {
                    is CheckOutcome.Updates -> getString(Res.string.qa_new_updates, outcome.count)
                    CheckOutcome.NoUpdates -> getString(Res.string.qa_no_updates)
                    CheckOutcome.SourcesFailed -> getString(Res.string.qa_sources_failed)
                    // No report was written, so there is nothing to view
                    CheckOutcome.CheckFailed -> getString(Res.string.qa_check_failed)
                }
                val view = if (outcome == CheckOutcome.SourcesFailed) getString(Res.string.qa_view) else null
                val progress = handle
                handle = null
                when (outcome) {
                    is CheckOutcome.Updates ->
                        if (progress != null) progress.success(title) else notifier.success(title)

                    CheckOutcome.NoUpdates ->
                        if (progress != null) progress.info(title) else notifier.info(title)

                    CheckOutcome.SourcesFailed ->
                        if (progress != null) progress.error(title, action = view, onAction = onViewAction)
                        else notifier.error(title, action = view, onAction = onViewAction)

                    CheckOutcome.CheckFailed ->
                        if (progress != null) progress.error(title) else notifier.error(title)
                }
                onResultDismissed()
            }
        }
    }

    val running = checkState as? ManualCheckState.Running
    val checked = running?.checked
    val total = running?.total
    val waiting = running?.waitingToStart == true
    val title = when {
        waiting -> stringResource(Res.string.qa_waiting_to_start)
        checked != null && total != null && total > 0 -> stringResource(Res.string.qa_checking, checked, total)
        else -> stringResource(Res.string.qa_checking_indeterminate)
    }
    LaunchedEffect(handle, title, checked, total) {
        val fraction = if (!waiting && checked != null && total != null && total > 0) checked.toFloat() / total else null
        if (running != null) handle?.update(title = title, fraction = fraction)
    }
}
