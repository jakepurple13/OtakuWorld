package com.programmersbox.kmpuiviews.utils.composables.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extension function to apply a privacy-sensitive effect when the app enters the recent apps menu.
 * Supports different privacy effects like blackout (redact) and blur.
 *
 * @param effect The privacy effect to apply. Defaults to `Redact(Color.Black)`.
 *
 * Usage Example:
 * ```
 * Modifier.privacySensitive() // Defaults to Redact(Black)
 * Modifier.privacySensitive(PrivacyEffect.Blur(10.dp)) // Applies a blur effect
 * Modifier.privacySensitive(PrivacyEffect.Redact(Color.Gray)) // Redacts with gray overlay
 * ```
 */
@Composable
fun Modifier.privacySensitive(effect: PrivacyEffect = PrivacyEffect.Redact()): Modifier {
    val windowInfo = LocalWindowInfo.current
    val isInRecentApps by rememberUpdatedState(!windowInfo.isWindowFocused)

    return if (isInRecentApps) {
        when (effect) {
            is PrivacyEffect.Redact -> applyRedact(effect.color)
            is PrivacyEffect.Blur -> applyBlur(effect.blurRadius)
        }
    } else {
        this
    }
}

/**
 * Applies a solid color overlay to redact the content when the app loses focus.
 *
 * @param color The color used for redaction. Default is black.
 */
private fun Modifier.applyRedact(color: Color = Color.Black) = drawWithContent {
    drawContent()
    drawRect(color)
}

/**
 * Applies a blur effect to obscure content when the app loses focus on Android 12+ (API 31+).
 * On older Android versions, where blur is not supported, it falls back
 * to redacting the content.
 *
 * @param blurRadius The radius of the blur effect in Dp. Default is 15.dp.
 */
fun Modifier.applyBlur(blurRadius: Dp = 15.dp): Modifier = this.blur(blurRadius)

/**
 * Sealed class defining different privacy effects.
 */
sealed class PrivacyEffect {
    /**
     * Redact effect applies a solid color overlay.
     *
     * @param color The color of the overlay. Default is black.
     */
    data class Redact(val color: Color = Color.Black) : PrivacyEffect()

    /**
     * Blur effect applies a blur filter.
     *
     * @param blurRadius The intensity of the blur effect. Default is 15.dp.
     */
    data class Blur(val blurRadius: Dp = 15.dp) : PrivacyEffect()
}