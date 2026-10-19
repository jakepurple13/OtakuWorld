package com.programmersbox.anime.shared.subtitles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Size, font, color, edge, background and raise controls.
 *
 * Every change is emitted as a field-wise transform, so it applies to the latest stored style
 * instead of overwriting it with the snapshot this composition saw.
 *
 * [onChange] persists a change. Sliders only call it when a drag ends; while dragging they call
 * [onDraft], which callers can use to show the in-progress value (it defaults to [onChange]).
 *
 * [compact] drops the section headers and dividers and tightens spacing, for tight containers.
 */
@Composable
fun SubtitleStyleControls(
    style: SubtitleStyle,
    onChange: (transform: (SubtitleStyle) -> SubtitleStyle) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onDraft: (transform: (SubtitleStyle) -> SubtitleStyle) -> Unit = onChange,
) {
    val sectionGap = if (compact) 12.dp else 20.dp
    Column(
        verticalArrangement = Arrangement.spacedBy(sectionGap),
        modifier = modifier
    ) {
        ControlSection(title = "Text", compact = compact) {
            StepSlider(
                label = "Size",
                valueLabel = "${style.sizePercent}%",
                value = style.sizePercent,
                range = 50..200,
                step = 10,
                onDraft = { v -> onDraft { it.copy(sizePercent = v) } },
                onCommit = { v -> onChange { it.copy(sizePercent = v) } },
            )
            FontChips(selected = style.font, onSelect = { f -> onChange { it.copy(font = f) } })
            ColorSwatches(selected = style.textColor, onSelect = { c -> onChange { it.copy(textColor = c) } })
        }

        if (!compact) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ControlSection(title = "Outline and background", compact = compact) {
            EdgeSegments(selected = style.edge, onSelect = { e -> onChange { it.copy(edge = e) } })
            StepSlider(
                label = "Background",
                valueLabel = if (style.backgroundPercent == 0) "Off" else "${style.backgroundPercent}%",
                value = style.backgroundPercent,
                range = 0..100,
                step = 5,
                onDraft = { v -> onDraft { it.copy(backgroundPercent = v) } },
                onCommit = { v -> onChange { it.copy(backgroundPercent = v) } },
            )
        }

        if (!compact) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ControlSection(title = "Position", compact = compact) {
            StepSlider(
                label = "Raise from bottom",
                valueLabel = "${style.raisePercent}%",
                value = style.raisePercent,
                range = 0..30,
                step = 1,
                onDraft = { v -> onDraft { it.copy(raisePercent = v) } },
                onCommit = { v -> onChange { it.copy(raisePercent = v) } },
            )
        }
    }
}

@Composable
internal fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun ControlSection(
    title: String,
    compact: Boolean,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
        if (!compact) SectionHeader(title)
        content()
    }
}

@Composable
private fun StepSlider(
    label: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    step: Int,
    onDraft: (Int) -> Unit,
    onCommit: (Int) -> Unit,
) {
    // Last value reached during the current drag; committed once when the drag ends.
    var dragged by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { new ->
                val snapped = ((new / step).roundToInt() * step)
                    .coerceIn(range)
                if (snapped != value) {
                    dragged = snapped
                    onDraft(snapped)
                }
            },
            onValueChangeFinished = {
                dragged?.let(onCommit)
                dragged = null
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first) / step - 1,
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontChips(selected: SubtitleFont, onSelect: (SubtitleFont) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Font", style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubtitleFont.entries.forEach { font ->
                FilterChip(
                    selected = font == selected,
                    onClick = { onSelect(font) },
                    label = { Text(font.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches(selected: SubtitleColor, onSelect: (SubtitleColor) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Color", style = MaterialTheme.typography.bodyLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SubtitleColor.entries.forEach { color ->
                Swatch(color = color, selected = color == selected, onClick = { onSelect(color) })
            }
        }
    }
}

@Composable
private fun Swatch(color: SubtitleColor, selected: Boolean, onClick: () -> Unit) {
    val fill = Color(color.argb.toInt())
    val base = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = color.name }
    val drawn = if (selected) {
        // Primary ring, then a surface-colored gap before the fill, as in the artboard.
        base
            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .padding(3.dp)
            .background(fill, CircleShape)
            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
    } else {
        base
            .background(fill, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    }
    Box(drawn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EdgeSegments(selected: SubtitleEdge, onSelect: (SubtitleEdge) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Edge", style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val edges = SubtitleEdge.entries
            edges.forEachIndexed { index, edge ->
                SegmentedButton(
                    selected = edge == selected,
                    onClick = { onSelect(edge) },
                    shape = SegmentedButtonDefaults.itemShape(index, edges.size),
                    icon = {},
                    label = { Text(edge.name, maxLines = 1) },
                )
            }
        }
    }
}
