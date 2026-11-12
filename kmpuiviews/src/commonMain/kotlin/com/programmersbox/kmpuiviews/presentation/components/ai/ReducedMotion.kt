package com.programmersbox.kmpuiviews.presentation.components.ai

import androidx.compose.runtime.Composable

/** True when the user turned system animations off; the AI components then draw statically. */
@Composable
expect fun rememberReducedMotion(): Boolean
