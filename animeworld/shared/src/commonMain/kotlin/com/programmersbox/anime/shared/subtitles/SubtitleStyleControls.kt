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
 * [compact] drops the section headers and dividers and tightens spacing, for tight containers.
 */
@Composable
fun SubtitleStyleControls(
    style: SubtitleStyle,
    onChange: (SubtitleStyle) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
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
                onValueChange = { onChange(style.copy(sizePercent = it)) },
            )
            FontChips(selected = style.font, onSelect = { onChange(style.copy(font = it)) })
            ColorSwatches(selected = style.textColor, onSelect = { onChange(style.copy(textColor = it)) })
        }

        if (!compact) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        ControlSection(title = "Outline and background", compact = compact) {
            EdgeSegments(selected = style.edge, onSelect = { onChange(style.copy(edge = it)) })
            StepSlider(
                label = "Background",
                valueLabel = if (style.backgroundPercent == 0) "Off" else "${style.backgroundPercent}%",
                value = style.backgroundPercent,
                range = 0..100,
                step = 5,
                onValueChange = { onChange(style.copy(backgroundPercent = it)) },
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
                onValueChange = { onChange(style.copy(raisePercent = it)) },
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
    onValueChange: (Int) -> Unit,
) {
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
                val snapped = (new / step).roundToInt() * step
                if (snapped != value) onValueChange(snapped.coerceIn(range))
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
