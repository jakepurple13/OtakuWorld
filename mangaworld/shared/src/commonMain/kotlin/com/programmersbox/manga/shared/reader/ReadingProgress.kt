package com.programmersbox.manga.shared.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Thin line at the top edge showing how far through the chapter the reader is, shown while the bars are
 * hidden. A page chip appears while scrolling and lingers briefly after. The line turns tertiary on the last page.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReadingProgressLine(
    progress: Float,
    visible: Boolean,
    pageLabel: String,
    scrolling: Boolean,
    modifier: Modifier = Modifier,
) {
    val shown by animateFloatAsState(progress, MaterialTheme.motionScheme.defaultSpatialSpec())
    val color by animateColorAsState(
        targetValue = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    var showChip by remember { mutableStateOf(false) }
    LaunchedEffect(scrolling) {
        if (scrolling) {
            showChip = true
        } else {
            delay(600)
            showChip = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth(shown)
                    .height(3.dp)
                    .background(color, RoundedEndShape)
            )
            AnimatedVisibility(
                visible = showChip,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    pageLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = .55f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private val RoundedEndShape = RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)
