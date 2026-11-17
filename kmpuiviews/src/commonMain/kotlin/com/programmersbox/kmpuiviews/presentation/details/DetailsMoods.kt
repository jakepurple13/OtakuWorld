package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.moodtags.Moods
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import com.programmersbox.kmpuiviews.presentation.components.ai.generatingBorder
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
)

/** A built-in mood's label, or the user's own mood as typed. */
@Composable
fun moodLabel(mood: String): String = Moods.label(mood)?.let { stringResource(it) } ?: mood

/** A dashed outline marks the user's own moods. */
fun Modifier.customMoodOutline(custom: Boolean, color: androidx.compose.ui.graphics.Color): Modifier =
    // Drawn over the content so a filled (selected) chip still shows the dashes
    if (!custom) this else drawWithContent {
        drawContent()
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))),
        )
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodRow(ui: MoodUi, title: String, modifier: Modifier = Modifier) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = ui.visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                stringResource(Res.string.moods_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ui.moods.forEach { mood ->
                    key(mood) {
                        val custom = !Moods.isBuiltIn(mood)
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .customMoodOutline(custom, MaterialTheme.colorScheme.onTertiaryContainer)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        ui.onRemove(mood)
                                    },
                                )
                        ) {
                            Text(
                                moodLabel(mood),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // The AI chip: only for favorites with no moods, when an engine is ready and the switch is on
                AnimatedVisibility(
                    visible = ui.canSuggest || ui.suggesting,
                    enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    val chipShape = AssistChipDefaults.shape
                    AssistChip(
                        onClick = { if (!ui.suggesting) ui.onSuggest() },
                        label = { Text(stringResource(Res.string.moods_suggest)) },
                        leadingIcon = { Spark(spinning = ui.suggesting, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.generatingBorder(ui.suggesting, chipShape),
                    )
                }

                AssistChip(
                    onClick = { showSheet = true },
                    label = { Text(stringResource(Res.string.moods_add)) },
                    leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                )
            }
            if (ui.moods.isNotEmpty()) {
                Text(
                    stringResource(Res.string.moods_remove_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showSheet) {
        MoodSheet(ui = ui, title = title, onDismiss = { showSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoodSheet(ui: MoodUi, title: String, onDismiss: () -> Unit) {
    val field = rememberTextFieldState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                (Moods.builtIn + ui.customMoods + ui.moods.filterNot { mood ->
                    Moods.isBuiltIn(mood) || ui.customMoods.any { it.equals(mood, ignoreCase = true) }
                }).forEach { mood ->
                    key(mood) {
                        val selected = ui.moods.any { it.equals(mood, ignoreCase = true) }
                        val custom = !Moods.isBuiltIn(mood)
                        FilterChip(
                            selected = selected,
                            onClick = { ui.onToggle(mood) },
                            label = { Text(moodLabel(mood)) },
                            border = if (custom) BorderStroke(0.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0f)) else null,
                            modifier = Modifier.customMoodOutline(custom, MaterialTheme.colorScheme.outline),
                        )
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
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.moods_done))
            }
        }
    }
}
