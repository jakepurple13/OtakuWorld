package com.programmersbox.anime.shared.episodeload

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.programmersbox.anime.shared.videochoice.VideoChoiceScreen
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice

/** Renders [loader]'s state: a full-screen overlay for Play, and the source sheet for both actions. */
@Composable
fun EpisodeLoadHost(loader: EpisodeLoader) {
    val state = loader.state

    // Keep the last state so the overlay can fade out after the loader clears it.
    var lastPlay by remember { mutableStateOf<EpisodeLoadState?>(null) }
    if (state != null && state.action == EpisodeAction.Play) lastPlay = state

    AnimatedVisibility(
        visible = state?.action == EpisodeAction.Play,
        enter = slideInVertically { it } + fadeIn(),
        exit = fadeOut(),
    ) {
        lastPlay?.let { EpisodeLoadingOverlay(it, loader::decide) }
    }

    val step = state?.step
    if (state != null && step is EpisodeLoadStep.ChooseSource) {
        VideoChoiceScreen(
            sources = step.sources,
            episodeName = state.episodeName,
            action = state.action,
            onPick = { loader.decide(EpisodeLoadDecision.Pick(it)) },
            onDismiss = { loader.decide(EpisodeLoadDecision.Cancel) },
        )
    }
}

private val Muted = Color.White.copy(alpha = 0.6f)

/** Remembers, per episode, whether the user was ever asked to choose and what they picked. */
private class ChecklistMemory {
    var sawChoose = false
    var label: String? = null
}

private enum class ItemState { Done, Current, Pending }

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun EpisodeLoadingOverlay(
    state: EpisodeLoadState,
    onDecision: (EpisodeLoadDecision) -> Unit,
) {
    if (state.action != EpisodeAction.Play) return

    val memory = remember(state.episodeUrl) { ChecklistMemory() }
    when (val step = state.step) {
        is EpisodeLoadStep.ChooseSource -> memory.sawChoose = true
        is EpisodeLoadStep.Resolving -> step.label?.let { memory.label = it }
        else -> Unit
    }

    BackHandler(enabled = true) { onDecision(EpisodeLoadDecision.Cancel) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Swallow touches so the screen underneath is not clickable.
            .pointerInput(Unit) { detectTapGestures { } }
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                    onDecision(EpisodeLoadDecision.Cancel)
                    true
                } else false
            }
    ) {
        state.posterUrl?.let {
            ImageLoaderChoice(
                imageUrl = it,
                name = state.episodeName,
                placeHolder = { ColorPainter(Color.Black) },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
                    .alpha(0.35f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            Box(
                Modifier
                    .widthIn(max = 120.dp)
                    .fillMaxWidth(0.4f)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                state.posterUrl?.let {
                    ImageLoaderChoice(
                        imageUrl = it,
                        name = state.episodeName,
                        placeHolder = { ColorPainter(Color.Transparent) },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                state.episodeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                state.showTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            AnimatedContent(
                targetState = state.step as? EpisodeLoadStep.Failed,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                contentAlignment = Alignment.TopCenter,
                label = "loadingOrFailed",
                modifier = Modifier.padding(top = 28.dp)
            ) { failed ->
                if (failed == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingIndicator(modifier = Modifier.size(48.dp))
                        Checklist(state.step, memory, Modifier.padding(top = 24.dp))
                    }
                } else {
                    FailedContent(failed, onDecision)
                }
            }
        }

        if (state.step !is EpisodeLoadStep.Failed) {
            TextButton(
                onClick = { onDecision(EpisodeLoadDecision.Cancel) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp)
            ) { Text("Cancel", color = Muted) }
        }
    }
}

@Composable
private fun Checklist(step: EpisodeLoadStep, memory: ChecklistMemory, modifier: Modifier = Modifier) {
    val items = buildList {
        add("Fetching sources")
        if (memory.sawChoose) add(memory.label ?: "Choose a source")
        add("Resolving stream")
    }
    val current = when (step) {
        is EpisodeLoadStep.FetchingSources -> 0
        is EpisodeLoadStep.ChooseSource -> 1
        else -> items.lastIndex
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        items.forEachIndexed { index, text ->
            ChecklistRow(
                text = text,
                state = when {
                    index < current -> ItemState.Done
                    index == current -> ItemState.Current
                    else -> ItemState.Pending
                }
            )
        }
    }
}

@Composable
private fun ChecklistRow(text: String, state: ItemState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            when (state) {
                ItemState.Done -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                ItemState.Current -> Box(
                    Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )

                ItemState.Pending -> Box(
                    Modifier
                        .size(10.dp)
                        .border(1.5.dp, Muted, CircleShape)
                )
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = when (state) {
                ItemState.Done -> Color.White
                ItemState.Current -> MaterialTheme.colorScheme.primary
                ItemState.Pending -> Muted
            }
        )
    }
}

@Composable
private fun FailedContent(failed: EpisodeLoadStep.Failed, onDecision: (EpisodeLoadDecision) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
        }
        Text(
            failed.message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        failed.detail?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(top = 24.dp)
                .widthIn(max = 280.dp)
        ) {
            Button(
                onClick = { onDecision(EpisodeLoadDecision.Retry) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Retry") }
            if (failed.canPickOther) {
                OutlinedButton(
                    onClick = { onDecision(EpisodeLoadDecision.PickOther) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Try another source") }
            }
            TextButton(
                onClick = { onDecision(EpisodeLoadDecision.Cancel) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Back", color = Muted) }
        }
    }
}
