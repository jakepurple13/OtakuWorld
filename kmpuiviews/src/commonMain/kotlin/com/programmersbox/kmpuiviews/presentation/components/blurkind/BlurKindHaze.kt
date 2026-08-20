package com.programmersbox.kmpuiviews.presentation.components.blurkind

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.programmersbox.datastore.NewSettingsHandling
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.material3.Material3
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.koinInject

@Composable
fun rememberBlurKindHazeState(
    dataStore: NewSettingsHandling = koinInject(),
): BlurKindHazeState {
    val useProgressive by dataStore.rememberUseProgressive()
    val hazeStyle = HazeBlurStyle.Material3()
    val hazeState = rememberHazeState()

    return remember(
        hazeState,
        hazeStyle,
        useProgressive
    ) {
        BlurKindHazeState(
            hazeState = hazeState,
            hazeStyle = hazeStyle,
            useProgressive = useProgressive
        )
    }
}

/**
 * Represents the state configuration for applying a blur effect with haze.
 *
 * @property hazeState Describes the haze parameters such as intensity or behavior within the blur effect.
 * @property hazeStyle Defines the visual style of the haze effect, controlling its appearance.
 * @property useProgressive Indicates whether the blur effect should be applied progressively.
 */
@Stable
class BlurKindHazeState(
    val hazeState: HazeState,
    val hazeStyle: HazeBlurStyle,
    val useProgressive: Boolean,
)
