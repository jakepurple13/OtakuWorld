package com.programmersbox.kmpuiviews.utils.composables

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Transition for swapping loading / content / empty states in an [androidx.compose.animation.AnimatedContent]:
 * the old state fades out quickly while the new one fades in with a slight scale, and the container
 * resizes instead of snapping.
 */
@Composable
fun <S> stateSwapTransition(): AnimatedContentTransitionScope<S>.() -> ContentTransform {
    val motion = MaterialTheme.motionScheme
    return {
        (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.96f)) togetherWith
                fadeOut(motion.fastEffectsSpec()) using SizeTransform(clip = false)
    }
}
