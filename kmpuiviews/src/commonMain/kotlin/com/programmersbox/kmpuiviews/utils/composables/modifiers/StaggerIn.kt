package com.programmersbox.kmpuiviews.utils.composables.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Parallax for pager content: while a page is being swiped it lags behind the pager and fades,
 * so the content drifts in instead of moving as one flat sheet. Driven by the gesture, no timing.
 * A higher [index] drifts further, for layering several children of one page.
 */
fun Modifier.pagerStagger(pagerState: PagerState, page: Int, index: Int): Modifier = graphicsLayer {
    val offset = pagerState.getOffsetDistanceInPages(page)
    translationX = offset * size.width * 0.12f * (index + 1)
    alpha = 1f - abs(offset).coerceAtMost(1f) * 0.6f
}

/**
 * Rises and fades the element in, [index] × 40ms after the screen opens (capped at 8 steps).
 * Plays once per visit: pass the same [played] flag, held in `rememberSaveable`, to every element
 * of the screen so coming back from a sub-screen doesn't replay it.
 */
@Composable
fun Modifier.staggerIn(index: Int, played: MutableState<Boolean>): Modifier {
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val shift = with(LocalDensity.current) { 16.dp.toPx() }
    val progress = remember { Animatable(if (played.value) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(40L * index.coerceAtMost(8))
            progress.animateTo(1f, spec)
            played.value = true
        }
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * shift
    }
}
