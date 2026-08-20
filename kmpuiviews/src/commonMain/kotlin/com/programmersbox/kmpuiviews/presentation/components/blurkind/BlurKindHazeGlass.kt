package com.programmersbox.kmpuiviews.presentation.components.blurkind

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.datastore.DataStoreHandlerObject
import com.programmersbox.datastore.NewSettingsHandling
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassDefaults
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.material3.Material3
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

val hazeGlassOptions = DataStoreHandlerObject<String, HazeOptionsInfo>(
    key = stringPreferencesKey("haze_glass_options"),
    defaultValue = HazeOptionsInfo(),
    mapToKey = { Json.encodeToString(it) },
    mapToType = { runCatching { Json.decodeFromString<HazeOptionsInfo>(it) }.getOrNull() }
)

@Serializable
data class HazeOptionsInfo(
    val refractionStrength: Float = 0.7f,
    val refractionHeightFraction: Float = 0.25f,
    val refractionDisplacement: Int = 15,
    val depth: Float = 1f,
    val blurRadius: Int = 14,

    val specularIntensity: Float = GlassDefaults.specularIntensity,
    val ambientResponse: Float = GlassDefaults.ambientResponse,
    val edgeSoftness: Int = GlassDefaults.edgeSoftness.value.toInt(),

    val chromaticAberrationStrength: Float = GlassDefaults.chromaticAberrationStrength,
    val chromaMultiplier: Float = GlassDefaults.chromaMultiplier,
    val specularExponent: Float = GlassDefaults.specularExponent,
    val fresnelExponent: Float = GlassDefaults.fresnelExponent,
)

@Composable
fun rememberBlurKindHazeGlassState(
    dataStore: NewSettingsHandling = koinInject(),
): BlurKindHazeGlassState {
    val hazeState = rememberHazeState()

    val handle by remember { hazeGlassOptions }
        .asFlow()
        .collectAsStateWithLifecycle(HazeOptionsInfo())

    val m3 = GlassStyle.Material3(
        tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    )

    return remember(
        hazeState,
        handle,
        m3
    ) {
        BlurKindHazeGlassState(
            hazeState = hazeState,
            hazeStyle = m3.then {
                optics(
                    refractionStrength = handle.refractionStrength,
                    refractionHeightFraction = handle.refractionHeightFraction,
                    refractionDisplacement = handle.refractionDisplacement.dp,
                    depth = handle.depth,
                    blurRadius = handle.blurRadius.dp
                )
                specularIntensity(handle.specularIntensity.coerceIn(0f..1f))
                ambientResponse(handle.ambientResponse.coerceIn(0f..1f))
                edgeSoftness(handle.edgeSoftness.dp)

                chromaticAberrationStrength(handle.chromaticAberrationStrength)
                chromaMultiplier(handle.chromaMultiplier.coerceIn(0f..2f))
                specularExponent(handle.specularExponent)
                fresnelExponent(handle.fresnelExponent)
                //shape(RoundedCornerShape(20.dp))
                //surfaceProfile(SurfaceProfile.Squircle)
            }.then {
                hovered {
                    animate(DefaultGlassHoverAnimationSpec, DefaultGlassReleaseAnimationSpec) {
                        lightingIntensity(0.35f)
                        refractionMultiplier(1.02f)
                        whitePointDelta(0.01f)
                    }
                }
                pressed {
                    animate(DefaultGlassPressAnimationSpec, DefaultGlassReleaseAnimationSpec) {
                        lightingIntensity(1f)
                        refractionMultiplier(1.08f)
                        whitePointDelta(0.04f)
                    }
                }
            },
        )
    }
}

internal val DefaultGlassHoverAnimationSpec: FiniteAnimationSpec<Float> = spring(
    dampingRatio = 1f,
    stiffness = Spring.StiffnessMediumLow,
)

internal val DefaultGlassPressAnimationSpec: FiniteAnimationSpec<Float> = spring(
    dampingRatio = 0.82f,
    stiffness = Spring.StiffnessMedium,
)

internal val DefaultGlassReleaseAnimationSpec: FiniteAnimationSpec<Float> = spring(
    dampingRatio = 0.72f,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * Represents the state configuration for applying a blur effect with haze.
 *
 * @property hazeState Describes the haze parameters such as intensity or behavior within the blur effect.
 * @property hazeStyle Defines the visual style of the haze effect, controlling its appearance.
 * @property useProgressive Indicates whether the blur effect should be applied progressively.
 */
@Stable
class BlurKindHazeGlassState(
    val hazeState: HazeState,
    val hazeStyle: GlassStyle,
)
