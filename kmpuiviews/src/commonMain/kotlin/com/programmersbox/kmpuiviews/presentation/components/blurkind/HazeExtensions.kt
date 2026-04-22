package com.programmersbox.kmpuiviews.presentation.components.blurkind

import androidx.compose.runtime.Composable
import com.programmersbox.datastore.BlurType
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun BlurType.toHazeStyle(): HazeStyle = when (this) {
    BlurType.Regular -> HazeMaterials.regular()
    BlurType.Thin -> HazeMaterials.thin()
    BlurType.Thick -> HazeMaterials.thick()
    BlurType.UltraThin -> HazeMaterials.ultraThin()
    else -> HazeMaterials.regular()
}
