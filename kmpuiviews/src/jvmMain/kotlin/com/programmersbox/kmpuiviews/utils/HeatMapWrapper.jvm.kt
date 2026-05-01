package com.programmersbox.kmpuiviews.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fleeys.heatmap.HeatMap
import com.fleeys.heatmap.model.Heat
import com.fleeys.heatmap.style.DaysLabelColor
import com.fleeys.heatmap.style.DaysLabelStyle
import com.fleeys.heatmap.style.HeatColor
import com.fleeys.heatmap.style.HeatMapStyle
import com.fleeys.heatmap.style.HeatStyle
import com.fleeys.heatmap.style.LabelStyle
import com.fleeys.heatmap.style.MonthsLabelColor
import com.fleeys.heatmap.style.MonthsLabelStyle

@Composable
actual fun HeatMapWrapper(
    data: List<KmpHeat<Int>>,
    onHeatClick: (KmpHeat<Int>) -> Unit,
    modifier: Modifier,
) {
    HeatMap(
        data = data.map { Heat(it.date, it.value, it.data) },
        onHeatClick = { onHeatClick(KmpHeat(it.date, it.value, it.data!!)) },
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
        style = HeatMapStyle().copy(
            heatStyle = HeatStyle().copy(
                heatColor = HeatColor().copy(
                    activeLowestColor = Color(0xff212f57),
                    activeHighestColor = MaterialTheme.colorScheme.primary,
                ),
                heatShape = CircleShape,
            ),
            labelStyle = LabelStyle().copy(
                daysLabelStyle = DaysLabelStyle(
                    color = DaysLabelColor(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ),
                monthsLabelStyle = MonthsLabelStyle(
                    color = MonthsLabelColor(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
        ),
    )
}
