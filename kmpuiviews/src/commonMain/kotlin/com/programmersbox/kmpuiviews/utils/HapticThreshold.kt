package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** True only on the step where [current] reaches [threshold] from below. */
fun thresholdCrossed(previous: Float, current: Float, threshold: Float): Boolean =
    previous < threshold && current >= threshold

/**
 * Ticks once each time a gesture's [fraction] reaches [threshold], such as pull-to-refresh or
 * slide-to-confirm passing the point where letting go commits the action.
 */
@Composable
fun ThresholdHaptic(fraction: () -> Float, threshold: Float = 1f) {
    val haptic = LocalHapticFeedback.current
    val currentFraction by rememberUpdatedState(fraction)
    LaunchedEffect(threshold) {
        var previous = currentFraction()
        snapshotFlow { currentFraction() }.collect { value ->
            if (thresholdCrossed(previous, value, threshold)) {
                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }
            previous = value
        }
    }
}
