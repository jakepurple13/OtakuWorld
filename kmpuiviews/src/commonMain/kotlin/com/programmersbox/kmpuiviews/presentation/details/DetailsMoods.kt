package com.programmersbox.kmpuiviews.presentation.details

import otakuworld.kmpuiviews.generated.resources.moods_remove_action
import otakuworld.kmpuiviews.generated.resources.moods_suggest_failed
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.Flow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.moodtags.Moods
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import com.programmersbox.kmpuiviews.presentation.components.ai.aiShimmer
import com.programmersbox.kmpuiviews.presentation.components.ai.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.moods_add
import otakuworld.kmpuiviews.generated.resources.moods_create
import otakuworld.kmpuiviews.generated.resources.moods_custom_hint
import otakuworld.kmpuiviews.generated.resources.moods_done
import otakuworld.kmpuiviews.generated.resources.moods_label
import otakuworld.kmpuiviews.generated.resources.moods_remove_hint
import otakuworld.kmpuiviews.generated.resources.moods_sheet_title
import otakuworld.kmpuiviews.generated.resources.moods_suggest
import otakuworld.kmpuiviews.generated.resources.moods_yours
import otakuworld.kmpuiviews.generated.resources.moods_yours_empty
import kotlin.math.roundToInt

/** What the Mood row needs from [MoodController], gathered by the screen like [TldrUi]. */
class MoodUi(
    val visible: Boolean,
    val moods: List<String>,
    val customMoods: List<String>,
    val canSuggest: Boolean,
    val suggesting: Boolean,
    val onSuggest: () -> Unit,
    val onToggle: (String) -> Unit,
    val onRemove: (String) -> Unit,
    val onCreate: (String) -> Unit,
    val suggestFailed: Flow<Unit>,
    val sheetOpen: Boolean,
    val onOpenSheet: () -> Unit,
    val onCloseSheet: () -> Unit,
)

/** A built-in mood's label, or the user's own mood as typed. */
@Composable
fun moodLabel(mood: String): String = Moods.label(mood)?.let { stringResource(it) } ?: mood

/** A dashed outline marks the user's own moods. */
fun Modifier.customMoodOutline(custom: Boolean, color: androidx.compose.ui.graphics.Color): Modifier =
    // Drawn over the content so a filled (selected) chip still shows the dashes
    if (!custom) this else drawWithContent {
        drawContent()
        // A sheet chip keeps its 48dp touch box, so hug the 32dp chip inside it
        val inset = ((size.height - FilterChipDefaults.Height.toPx()) / 2).coerceAtLeast(0f)
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, inset),
            size = Size(size.width, size.height - inset * 2),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))),
        )
    }

private const val SLOT_COUNT = 3
private val SlotWidths = listOf(86.dp, 64.dp, 98.dp)
private const val ENTER_STAGGER_MS = 70
private const val RESOLVE_STAGGER_MS = 90L
private const val EXIT_MS = 160
private val PopEasing = CubicBezierEasing(0.34f, 1.32f, 0.64f, 1f)

/**
 * A mood in the row. Kept after removal until its exit animation ends. [slot] is the placeholder it
 * resolves from when the AI suggested it.
 */
private class RowMood(val mood: String, val shown: MutableTransitionState<Boolean>, val delayMs: Int, val slot: Int?)

/** Moves the row toward [target]: removed moods exit first, then new ones enter one after another. */
private fun SnapshotStateList<RowMood>.sync(target: List<String>, animate: Boolean, fromSlots: Boolean) {
    val removed = filter { e -> e.shown.targetState && target.none { it == e.mood } }
    removed.forEach { if (animate) it.shown.targetState = false else remove(it) }
    val present = filter { it.shown.targetState }.map { it.mood }
    val delayBase = if (animate && removed.isNotEmpty()) EXIT_MS else 0
    var added = 0
    target.forEachIndexed { index, mood ->
        if (mood in present) return@forEachIndexed
        val slot = if (fromSlots && added < SLOT_COUNT) added else null
        val entry = RowMood(
            mood = mood,
            // A resolving slot is already on screen, so its chip starts shown and morphs instead
            shown = MutableTransitionState(!animate || slot != null).apply { targetState = true },
            delayMs = delayBase + added * ENTER_STAGGER_MS,
            slot = slot,
        )
        added++
        val previous = target.getOrNull(index - 1)
        val at = if (previous == null) 0 else indexOfFirst { it.mood == previous && it.shown.targetState } + 1
        add(at.coerceIn(0, size), entry)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MoodRow(ui: MoodUi, title: String, modifier: Modifier = Modifier) {
    val notifier = koinInject<InAppNotifier>()
    val failedText = stringResource(Res.string.moods_suggest_failed)
    LaunchedEffect(ui.suggestFailed) { ui.suggestFailed.collect { notifier.info(failedText) } }

    val reducedMotion = rememberReducedMotion()
    val entries = remember {
        mutableStateListOf<RowMood>().apply { ui.moods.forEach { add(RowMood(it, MutableTransitionState(true), 0, null)) } }
    }
    // Only the user's own changes animate; loading the row or a change from elsewhere just shows the result
    var animateNext by remember { mutableStateOf(false) }
    LaunchedEffect(ui.sheetOpen) { if (ui.sheetOpen) animateNext = true }
    // Slots stay up until the suggested moods take their place. Not tied to ui.suggesting alone: the
    // moods and the end of suggesting can land in the same frame.
    var slotsShowing by remember { mutableStateOf(false) }
    LaunchedEffect(ui.suggesting) {
        if (ui.suggesting) slotsShowing = true else if (ui.moods.isEmpty()) slotsShowing = false
    }
    // The sheet covers the row, so its changes wait until it has slid away
    LaunchedEffect(ui.moods, ui.sheetOpen) {
        if (ui.sheetOpen) return@LaunchedEffect
        val animate = !reducedMotion && (animateNext || slotsShowing)
        entries.sync(ui.moods, animate = animate, fromSlots = animate && slotsShowing)
        if (ui.moods.isNotEmpty()) slotsShowing = false
        animateNext = false
    }
    val resolvedSlots = entries.count { it.slot != null && it.shown.targetState }

    AnimatedVisibility(
        visible = ui.visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .animateContentSize()
        ) {
            Text(
                stringResource(Res.string.moods_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LookaheadScope {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    entries.forEach { entry ->
                        key(entry) {
                            AnimatedVisibility(
                                visibleState = entry.shown,
                                enter = scaleIn(tween(420, entry.delayMs, PopEasing), initialScale = 0.6f) +
                                    fadeIn(tween(200, entry.delayMs)),
                                exit = scaleOut(tween(EXIT_MS), targetScale = 0.6f) + fadeOut(tween(EXIT_MS)),
                            ) {
                                Glide {
                                    MoodChip(
                                        mood = entry.mood,
                                        slot = entry.slot,
                                        onRemove = {
                                            animateNext = true
                                            ui.onRemove(entry.mood)
                                        },
                                    )
                                }
                            }
                            if (entry.shown.isIdle && !entry.shown.currentState && !entry.shown.targetState) {
                                LaunchedEffect(entry) { entries.remove(entry) }
                            }
                        }
                    }

                    // While the AI works, shimmering slots hold the places the moods will land in.
                    // A slot leaves the row the moment its mood arrives; spare ones shrink away.
                    for (i in resolvedSlots until SLOT_COUNT) {
                        key("slot-$i") {
                            val delay = 140 + i * 60
                            AnimatedVisibility(
                                visible = slotsShowing,
                                enter = expandHorizontally(tween(320, delay), expandFrom = Alignment.Start) + fadeIn(tween(220, delay)),
                                exit = shrinkHorizontally(tween(200)) + fadeOut(tween(150)),
                            ) {
                                Glide { MoodSlot(SlotWidths[i]) }
                            }
                        }
                    }

                    // The AI chip: only for favorites with no moods, when an engine is ready and the switch is on.
                    // Its return waits for the spare slots to shrink away, so it doesn't land on top of them.
                    AnimatedVisibility(
                        visible = ui.canSuggest,
                        enter = scaleIn(tween(350, 200, PopEasing), initialScale = 0.8f) + fadeIn(tween(200, 200)),
                        exit = scaleOut(tween(150), targetScale = 0.8f) + fadeOut(tween(150)),
                    ) {
                        Glide {
                            AssistChip(
                                onClick = ui.onSuggest,
                                label = { Text(stringResource(Res.string.moods_suggest)) },
                                leadingIcon = { Spark(spinning = false, modifier = Modifier.size(16.dp)) },
                            )
                        }
                    }

                    Glide {
                        AssistChip(
                            onClick = ui.onOpenSheet,
                            label = { Text(stringResource(Res.string.moods_add)) },
                            leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = entries.any { it.shown.targetState },
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Text(
                    stringResource(Res.string.moods_remove_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (ui.sheetOpen) {
        MoodSheet(ui = ui, title = title, onDismiss = ui.onCloseSheet)
    }
}

/**
 * Slides the item to its new place when the row reflows. Only the box's bounds animate; the item
 * still measures itself, so a chip's 48dp touch box doesn't get stretched mid-move.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LookaheadScope.Glide(content: @Composable () -> Unit) {
    Box(Modifier.animateBounds(this), contentAlignment = Alignment.Center) { content() }
}

/**
 * A mood in the row. With a [slot], it starts looking like that placeholder and resolves: the fill
 * and text fade in while the width springs to fit.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoodChip(mood: String, slot: Int?, onRemove: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val custom = !Moods.isBuiltIn(mood)
    val reveal = remember { Animatable(if (slot == null) 1f else 0f) }
    val grow = remember { Animatable(if (slot == null) 1f else 0f) }
    if (slot != null) {
        LaunchedEffect(Unit) {
            delay(slot * RESOLVE_STAGGER_MS)
            launch { reveal.animateTo(1f, tween(260)) }
            grow.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val startWidth = SlotWidths[slot ?: 0]
    val removeLabel = stringResource(Res.string.moods_remove_action)
    Surface(
        color = lerp(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.tertiaryContainer, reveal.value),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .customMoodOutline(custom && reveal.value == 1f, MaterialTheme.colorScheme.onTertiaryContainer)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {},
                onLongClickLabel = removeLabel,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemove()
                },
            )
    ) {
        Text(
            moodLabel(mood),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .growFrom(startWidth) { grow.value }
                .graphicsLayer { alpha = reveal.value }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** Lays the content out [progress] of the way from [start] wide to its own width, centred. */
private fun Modifier.growFrom(start: Dp, progress: () -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val from = start.roundToPx()
    val width = (from + (placeable.width - from) * progress()).roundToInt().coerceAtLeast(0)
    layout(width, placeable.height) { placeable.place((width - placeable.width) / 2, 0) }
}

/** A shimmering placeholder where an AI-suggested mood will land. */
@Composable
private fun MoodSlot(width: Dp) {
    Box(
        Modifier
            .padding(vertical = 4.dp)
            .size(width, 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .aiShimmer()
    )
}

@Composable
private fun MoodFilterChip(mood: String, selected: Boolean, onClick: () -> Unit) {
    val custom = !Moods.isBuiltIn(mood)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(moodLabel(mood)) },
        // Always present, so the chip's padding stays put while the check grows in and out
        leadingIcon = {
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(expandFrom = Alignment.Start) + scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + scaleOut() + fadeOut(),
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize))
            }
        },
        border = if (custom) BorderStroke(0.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0f)) else null,
        modifier = Modifier.customMoodOutline(custom, MaterialTheme.colorScheme.outline),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoodSheet(ui: MoodUi, title: String, onDismiss: () -> Unit) {
    val field = rememberTextFieldState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val selected = { mood: String -> ui.moods.any { it.equals(mood, ignoreCase = true) } }
    val ownMoods = ui.customMoods + ui.moods.filterNot { mood ->
        Moods.isBuiltIn(mood) || ui.customMoods.any { it.equals(mood, ignoreCase = true) }
    }
    // Moods made while the sheet is open pop in; the ones already there just show
    val shownAtOpen = remember { ownMoods.toSet() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(Res.string.moods_sheet_title, title), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Moods.builtIn.forEach { mood ->
                    key(mood) { MoodFilterChip(mood, selected(mood)) { ui.onToggle(mood) } }
                }
            }
            HorizontalDivider()
            // The user's own moods sit with the field that makes them
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.animateContentSize()
            ) {
                Text(
                    stringResource(Res.string.moods_yours).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (ownMoods.isEmpty()) {
                    Text(
                        stringResource(Res.string.moods_yours_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ownMoods.forEach { mood ->
                            key(mood) {
                                val shown = remember { MutableTransitionState(mood in shownAtOpen).apply { targetState = true } }
                                AnimatedVisibility(
                                    visibleState = shown,
                                    enter = scaleIn(tween(420, easing = PopEasing), initialScale = 0.6f) + fadeIn(tween(200)),
                                ) {
                                    MoodFilterChip(mood, selected(mood)) { ui.onToggle(mood) }
                                }
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        state = field,
                        placeholder = { Text(stringResource(Res.string.moods_custom_hint)) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            ui.onCreate(field.text.toString())
                            field.clearText()
                        },
                        enabled = field.text.isNotBlank(),
                    ) { Text(stringResource(Res.string.moods_create)) }
                }
            }
            Button(
                // Slide the sheet away first, so the row's animation plays once the row is visible again
                onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(Res.string.moods_done))
            }
        }
    }
}
