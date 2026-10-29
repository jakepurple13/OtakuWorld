package com.programmersbox.kmpuiviews.presentation.details

import com.materialkolor.PaletteStyle
import androidx.compose.ui.graphics.Color
import com.programmersbox.kmpuiviews.utils.composables.modifiers.fadeInAnimation
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * The loaded Details screen is built in stages, one per frame, so its first composition is spread
 * out instead of landing in one long frame. The cover and title show before any stage.
 */
internal object DetailsRevealStage {
    /** Source, favorite button, chapter count */
    const val Header = 1

    /** Genre chips and description */
    const val Info = 2

    /** Chapter list and its actions */
    const val Chapters = 3

    const val All = Chapters
}

/** Defaults to fully revealed so anything outside the Details screen is unaffected */
internal val LocalDetailsRevealStage = compositionLocalOf { DetailsRevealStage.All }

@Composable
internal fun isRevealed(stage: Int) = LocalDetailsRevealStage.current >= stage

/** False when the screen was restored already revealed, so nothing fades in again */
internal val LocalDetailsRevealAnimates = compositionLocalOf { true }

/** Fades a stage in as it's revealed */
@Composable
internal fun Modifier.revealFade(): Modifier =
    if (LocalDetailsRevealAnimates.current) fadeInAnimation(DetailsRevealFadeMillis) else this

/** Fade used for each stage as it comes in */
internal const val DetailsRevealFadeMillis = 220

/** Chapter rows fade in one after another; only the first screenful is staggered */
internal fun chapterRevealFade(index: Int): FiniteAnimationSpec<Float> =
    tween(DetailsRevealFadeMillis, delayMillis = index.coerceAtMost(8) * 50)

/** What a Details color scheme is built from; the ViewModel caches the last scheme by these */
internal data class SchemeInputs(
    val seedColor: Color,
    val isDark: Boolean,
    val isAmoled: Boolean,
    val style: PaletteStyle,
)
