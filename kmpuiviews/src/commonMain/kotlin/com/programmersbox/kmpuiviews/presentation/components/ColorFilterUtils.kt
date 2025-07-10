package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import com.programmersbox.datastore.ColorBlindnessType

// --- Color Blindness Simulation Matrices ---
// These matrices are based on common models for simulating color vision deficiencies.
// They transform RGB values to simulate how they would appear to someone with the condition.
private fun ColorBlindnessType.getMatrix() = when (this) {
    ColorBlindnessType.None -> null
    // Protanomaly/Protanopia (Red-Green color blindness, red cones are defective/missing)
    // Values derived from: http://www.daltonize.org/
    ColorBlindnessType.Protanopia -> floatArrayOf(
        0.567F, 0.433F, 0.0F, 0.0F, 0.0F,
        0.558F, 0.442F, 0.0F, 0.0F, 0.0F,
        0.0F, 0.242F, 0.758F, 0.0F, 0.0F,
        0.0F, 0.0F, 0.0F, 1.0F, 0.0F
    )
    // Deuteranomaly/Deuteranopia (Red-Green color blindness, green cones are defective/missing)
    // Values derived from: http://www.daltonize.org/
    ColorBlindnessType.Deuteranopia -> floatArrayOf(
        0.625F, 0.375F, 0.0F, 0.0F, 0.0F,
        0.7F, 0.3F, 0.0F, 0.0F, 0.0F,
        0.0F, 0.3F, 0.7F, 0.0F, 0.0F,
        0.0F, 0.0F, 0.0F, 1.0F, 0.0F
    )
    // Tritanomaly/Tritanopia (Blue-Yellow color blindness, blue cones are defective/missing)
    // Values derived from: http://www.daltonize.org/
    ColorBlindnessType.Tritanopia -> floatArrayOf(
        0.95F, 0.05F, 0.0F, 0.0F, 0.0F,
        0.0F, 0.433F, 0.567F, 0.0F, 0.0F,
        0.0F, 0.475F, 0.525F, 0.0F, 0.0F,
        0.0F, 0.0F, 0.0F, 1.0F, 0.0F
    )
}

fun colorFilterBlind(colorBlindnessType: ColorBlindnessType): ColorFilter? {
    return colorBlindnessType
        .getMatrix()
        ?.let {
            val colorMatrix = ColorMatrix(it)
            ColorMatrixColorFilter(colorMatrix)
        }
}