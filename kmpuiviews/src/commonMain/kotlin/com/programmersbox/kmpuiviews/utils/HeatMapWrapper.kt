package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate

data class KmpHeat<T>(
    val date: LocalDate,
    val value: Double,
    val data: T,
)

@Composable
expect fun HeatMapWrapper(
    data: List<KmpHeat<Int>>,
    onHeatClick: (KmpHeat<Int>) -> Unit,
    modifier: Modifier = Modifier,
)
