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
import com.programmersbox.datastore.BlurKind
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.components.custombackdrop.backdrops.layerBackdrop
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyleScope
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import org.koin.compose.koinInject

/**
 * Creates and remembers the state for managing blur-related UI configurations, such as haze styles,
 * liquid effects, and other blur properties. This function leverages composable and reactive
 * elements to provide a consistent UI state that can be reused across recompositions.
 *
 * @param dataStore The DataStore instance used to retrieve and manage settings or preferences
 *                  required for configuring the blur state. Defaults to a `koinInject()` resolved instance.
 * @param backgroundColor The background color used for rendering the backdrop layer
 *                        in blur-related effects. Defaults to `MaterialTheme.colorScheme.surface`.
 * @return A [BlurKindState] object encapsulating various blur configurations including haze and
 *         liquid blur states, ensuring dynamic and reactive behavior.
 */
@Composable
fun rememberBlurKindState(
    dataStore: NewSettingsHandling = koinInject(),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
): BlurKindState {
    val showBlur by dataStore.rememberShowBlur()
    val blurKind by dataStore.rememberBlurKind()
    val blurKindHazeState = rememberBlurKindHazeState(
        dataStore = dataStore,
    )
    val blurKindLiquidState = rememberBlurKindLiquidState(
        dataStore = dataStore,
        backgroundColor = backgroundColor
    )
    val blurKindHazeGlassState = rememberBlurKindHazeGlassState(
        dataStore = dataStore,
    )

    return remember(
        blurKind,
        showBlur,
        blurKindLiquidState,
        blurKindHazeState,
        blurKindHazeGlassState
    ) {
        BlurKindState(
            blurKind = blurKind,
            showBlur = showBlur,
            hazeState = blurKindHazeState,
            hazeGlassState = blurKindHazeGlassState,
            liquidState = blurKindLiquidState
        )
    }
}

/**
 * Represents the state for managing and displaying blur effects of different kinds.
 *
 * This class provides configuration and state management for two main types of blur effects: haze and liquid glass.
 * It allows for toggling the visibility of the blur effect and provides detailed state objects for customizing
 * the behavior and appearance of each blur kind.
 *
 * @constructor Creates a new instance of BlurKindState.
 *
 * @property blurKind Specifies the type of blur effect to apply, either `Haze` or `LiquidGlass`.
 * @property showBlur Determines whether the blur effect should be displayed or not.
 * @property hazeState Holds the state and configuration specific to the `Haze` blur effect, including style and behavior.
 * @property liquidState Contains the settings specific to the `LiquidGlass` blur effect, such as refraction and chromatic properties.
 */
@Stable
class BlurKindState(
    val blurKind: BlurKind,
    val showBlur: Boolean,
    val hazeState: BlurKindHazeState,
    val hazeGlassState: BlurKindHazeGlassState,
    val liquidState: BlurKindLiquidState,
)

/**
 * Configures the modifier to apply a blur effect based on the specified blur kind and its associated properties.
 *
 * @param blurKindState The state object that determines the type of blur effect, its visibility, and specific properties for rendering the effect.
 * @param liquidGlassShape A lambda function specifying the shape for the liquid glass effect. Defaults to a rounded corner shape with a radius of 1
 * dp.
 * @param hazeScope A lambda function defining additional configurations for the haze effect. This block is executed if the blur kind is set to Haze
 * .
 */
fun Modifier.setBlurKind(
    blurKindState: BlurKindState,
    liquidGlassShape: () -> Shape = { RoundedCornerShape(1.dp) },
    hazeScope: HazeBlurStyleScope.() -> Unit = {},
) = if (blurKindState.showBlur) {
    when (blurKindState.blurKind) {
        BlurKind.Haze -> hazeBlur(
            input = HazeInput.Sources(blurKindState.hazeState.hazeState),
            style = blurKindState.hazeState.hazeStyle.then {
                hazeScope()
                blurEnabled(blurKindState.showBlur)
                if (!blurKindState.hazeState.useProgressive) {
                    progressive(null)
                }
            }
        )

        BlurKind.HazeGlass -> hazeGlass(
            input = HazeInput.Sources(blurKindState.hazeGlassState.hazeState),
            style = blurKindState.hazeGlassState.hazeStyle.then {
                shape(liquidGlassShape() as RoundedCornerShape)
            }
        )
        /*hazeEffect(state = blurKindState.hazeState.hazeState) {
        blurEffect {
            style = blurKindState.hazeState.hazeStyle
            blurEnabled = blurKindState.showBlur
            hazeScope()
            if (!blurKindState.hazeState.useProgressive) {
                progressive = null
            }
        }
    }*/

        BlurKind.LiquidGlass -> liquidGlassBlur(
            blurKindState = blurKindState,
            liquidGlassShape = liquidGlassShape
        )
        /*hazeGlass(
        input = HazeInput.Sources(blurKindState.hazeState.hazeState),
        style = GlassStyle {
            backgroundColor(blurKindState.liquidState.backgroundColor)
            shape(liquidGlassShape() as RoundedCornerShape)
            optics(
                refractionHeightFraction = blurKindState.liquidState.refractionHeight,
                refractionStrength = blurKindState.liquidState.refractionAmount,
            )
            chromaticAberrationMode(
                if(blurKindState.liquidState.chromaticAberration) {
                    ChromaticAberrationMode.Full
                } else {
                    ChromaticAberrationMode.Simple
                }
            )
        }
    )*/
        /*
        drawBackdrop(
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
         */

        /*liquidGlassBlur(
        blurKindState = blurKindState,
        liquidGlassShape = liquidGlassShape
    )*/
    }
} else this

/**
 * Applies a specific type of blur effect to the Modifier based on the provided blurKindState.
 *
 * @param blurKindState The state object that determines the type of blur to apply
 * and whether to show the blur. It contains details about the selected blur kind
 * and its associated configuration.
 */
fun Modifier.setBlurKindSource(blurKindState: BlurKindState) = if (blurKindState.showBlur) {
    when (blurKindState.blurKind) {
        BlurKind.Haze -> hazeSource(blurKindState.hazeState.hazeState)
        BlurKind.HazeGlass -> hazeSource(blurKindState.hazeGlassState.hazeState)
        BlurKind.LiquidGlass -> layerBackdrop(blurKindState.liquidState.backdrop)
    }
} else this

/**
 * Applies a blur effect to the FloatingActionButton based on the specified blur kind and shape.
 *
 * The method modifies the UI representation of a component by applying one of the blur effects
 * defined in the `BlurKind` enum (`Haze` or `LiquidGlass`). It uses the styling provided by
 * `BlurKindState` and performs additional visual adjustments when the `LiquidGlass` blur kind is selected.
 *
 * @param blurKindState The state object that determines the active blur kind, visibility, and specific
 *                      configuration details for the blur effect.
 * @param shape The shape of the FloatingActionButton which determines the outline boundary where
 *              the blur effect is applied.
 */
fun Modifier.floatingActionButtonBlurKind(
    blurKindState: BlurKindState,
    shape: Shape,
    customBlurAmount: Float = 1f,
) = if (blurKindState.showBlur) {
    when (blurKindState.blurKind) {
        BlurKind.Haze -> this
        BlurKind.HazeGlass -> this
        BlurKind.LiquidGlass -> liquidGlassFABBlur(
            blurKindState = blurKindState,
            customBlurAmount = customBlurAmount,
            shape = shape
        )
    }
} else this