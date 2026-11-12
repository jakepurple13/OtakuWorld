package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Thin line at the top edge showing how far through the chapter the reader is, shown while the bars are
 * hidden. It springs to each new value. When [pageLabel] is set, a page
 * chip shows while [showChip] is true. The line turns tertiary on the last page.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReadingProgressLine(
    progress: Float,
    visible: Boolean,
    pageLabel: String?,
    showChip: Boolean,
    modifier: Modifier = Modifier,
) {
    val shown by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
    )
    val color by animateColorAsState(
        targetValue = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth(shown.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(color, RoundedEndShape)
            )
            if (pageLabel != null) {
                AnimatedVisibility(
                    visible = showChip,
                    enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) { PageChip(pageLabel) }
            }
        }
    }
}

/**
 * The top line split into one segment per page, like a stories viewer. Segments up to the current page are
 * filled; the current one fills in as you arrive. A page chip shows while [showChip] is true.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SegmentedProgressLine(
    index: Int,
    pages: Int,
    showChip: Boolean,
    onChipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (index >= pages - 1) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    val fillSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(pages) { i ->
                val fill = animateFloatAsState(if (i <= index) 1f else 0f, fillSpec)
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .drawBehind {
                            drawRect(SegmentTrack)
                            drawRect(color, size = size.copy(width = size.width * fill.value))
                        }
                )
            }
        }
        AnimatedVisibility(
            visible = showChip,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + slideInVertically { -it / 2 },
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) + slideOutVertically { -it / 2 },
        ) { PageChip(pageLabel(index, pages), Modifier.clickable(onClick = onChipClick)) }
    }
}

@Composable
private fun PageChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .clip(CircleShape)
            .then(modifier)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = .55f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

private val RoundedEndShape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
private val SegmentTrack = Color.Black.copy(alpha = .15f)
