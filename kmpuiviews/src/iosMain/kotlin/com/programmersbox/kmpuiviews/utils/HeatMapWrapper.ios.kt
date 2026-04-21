package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun HeatMapWrapper(
    data: List<KmpHeat<Int>>,
    onHeatClick: (KmpHeat<Int>) -> Unit,
    modifier: Modifier,
) {
}
