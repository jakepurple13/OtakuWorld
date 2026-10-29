package com.programmersbox.kmpuiviews.utils.composables.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.fadeInAnimation(
    durationMillis: Int = 600,
    delayMillis: Int = 0,
): Modifier = composed {
    val animatedProgress = remember { Animatable(initialValue = 0f) }
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, delayMillis)
        )
    }
    // Read in the draw phase so the fade doesn't recompose the content every frame
    graphicsLayer { alpha = animatedProgress.value }
}
