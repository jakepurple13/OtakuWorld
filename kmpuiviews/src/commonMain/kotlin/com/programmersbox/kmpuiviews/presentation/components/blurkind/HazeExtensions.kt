package com.programmersbox.kmpuiviews.presentation.components.blurkind

import androidx.compose.runtime.Composable
import com.programmersbox.datastore.BlurType
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.materials.HazeMaterials

@Composable
fun BlurType.toHazeStyle(): HazeBlurStyle = when (this) {
    BlurType.Regular -> HazeMaterials.regular()
    BlurType.Thin -> HazeMaterials.thin()
    BlurType.Thick -> HazeMaterials.thick()
    BlurType.UltraThin -> HazeMaterials.ultraThin()
}
