package com.programmersbox.kmpuiviews.presentation.onboarding.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.programmersbox.kmpuiviews.presentation.components.ai.rememberReducedMotion

/** Lets tests (and previews) force reduced motion on or off regardless of the system setting. */
val LocalReducedMotionOverride = staticCompositionLocalOf<Boolean?> { null }

@Composable
fun onboardingReducedMotion(): Boolean = LocalReducedMotionOverride.current ?: rememberReducedMotion()
