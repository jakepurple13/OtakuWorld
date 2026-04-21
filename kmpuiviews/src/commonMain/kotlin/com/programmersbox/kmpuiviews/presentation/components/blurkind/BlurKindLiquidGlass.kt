package com.programmersbox.kmpuiviews.presentation.components.blurkind

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.programmersbox.datastore.NewSettingsHandling
import org.koin.compose.koinInject

@Composable
fun rememberBlurKindLiquidState(
    dataStore: NewSettingsHandling = koinInject(),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
): BlurKindLiquidState {
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    val liquidGlassBlurAmount by dataStore.rememberLiquidGlassBlurAmount()
    val liquidGlassRefractionHeight by dataStore.rememberLiquidGlassRefractionHeight()
    val liquidGlassRefractionAmount by dataStore.rememberLiquidGlassRefractionAmount()
    val liquidGlassDepthEffect by dataStore.rememberLiquidGlassDepthEffect()
    val liquidGlassChromaticAberration by dataStore.rememberLiquidGlassChromaticAberration()

    return remember(
        backdrop,
        liquidGlassBlurAmount,
        liquidGlassRefractionHeight,
        liquidGlassRefractionAmount,
        liquidGlassDepthEffect,
        liquidGlassChromaticAberration
    ) {
        BlurKindLiquidState(
            backdrop = backdrop,
            backgroundColor = backgroundColor,
            blurAmount = liquidGlassBlurAmount,
            refractionHeight = liquidGlassRefractionHeight,
            refractionAmount = liquidGlassRefractionAmount,
            depthEffect = liquidGlassDepthEffect,
            chromaticAberration = liquidGlassChromaticAberration,
        )
    }
}

/**
 * Represents a state for rendering a liquid-like blur effect.
 *
 * @constructor Constructs a BlurKindLiquidState instance with the specified parameters.
 *
 * @property backdrop The backdrop layer used as the source for the blur effect.
 * @property backgroundColor The color applied to the background of the effect.
 * @property blurAmount The intensity of the blur effect.
 * @property refractionHeight The height used to simulate refraction in the effect.
 * @property refractionAmount The strength or magnitude of the refraction effect.
 * @property depthEffect Indicates if a depth effect should be applied.
 * @property chromaticAberration Determines if chromatic aberration is enabled for the effect.
 */
@Stable
class BlurKindLiquidState(
    val backdrop: LayerBackdrop,
    val backgroundColor: Color,
    val blurAmount: Float,
    val refractionHeight: Float,
    val refractionAmount: Float,
    val depthEffect: Boolean,
    val chromaticAberration: Boolean,
)

fun Modifier.liquidGlassBlur(
    blurKindState: BlurKindState,
    liquidGlassShape: () -> Shape = { RoundedCornerShape(1.dp) },
) = drawBackdrop(
    backdrop = blurKindState.liquidState.backdrop,
    shape = liquidGlassShape,
    effects = {
        vibrancy()
        blur(blurKindState.liquidState.blurAmount.dp.toPx())
        lens(
            refractionHeight = blurKindState.liquidState.refractionHeight.dp.toPx(),
            refractionAmount = blurKindState.liquidState.refractionAmount.dp.toPx(),
            depthEffect = blurKindState.liquidState.depthEffect,
            chromaticAberration = blurKindState.liquidState.chromaticAberration
        )
    },
    onDrawSurface = { drawRect(blurKindState.liquidState.backgroundColor.copy(alpha = 0.5f)) },
    highlight = { Highlight.Ambient }
)

fun Modifier.liquidGlassFABBlur(
    blurKindState: BlurKindState,
    customBlurAmount: Float = 1f,
    shape: Shape,
) = drawBackdrop(
    backdrop = blurKindState.liquidState.backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(customBlurAmount.dp.toPx())
        lens(
            refractionHeight = 16.dp.toPx(),
            refractionAmount = 38.dp.toPx(),
            depthEffect = true,
            chromaticAberration = true
        )
    },
    onDrawSurface = {
        drawRect(
            blurKindState
                .liquidState
                .backgroundColor
                .copy(alpha = 0.5f)
        )
    },
    highlight = { Highlight.Plain },
    shadow = null
)