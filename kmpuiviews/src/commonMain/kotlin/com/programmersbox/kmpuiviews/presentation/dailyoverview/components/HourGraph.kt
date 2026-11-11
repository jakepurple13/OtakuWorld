package com.programmersbox.kmpuiviews.presentation.dailyoverview.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_by_hour
import otakuworld.kmpuiviews.generated.resources.do_hour_axis
import otakuworld.kmpuiviews.generated.resources.do_most_at

/** "10 PM" for the busiest hour (the later one on a tie), or null when nothing was read. */
fun peakHourLabel(readsByHour: List<Int>): String? {
    val max = readsByHour.maxOrNull() ?: return null
    if (max <= 0) return null
    val hour = readsByHour.lastIndexOf(max)
    val twelveHour = if (hour % 12 == 0) 12 else hour % 12
    return "$twelveHour ${if (hour < 12) "AM" else "PM"}"
}

private val BarEasing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.1f)
private const val BarStartDelayMs = 520L
private const val BarStaggerMs = 25L

/** 24 bars, one per hour, that grow in with a 25ms stagger. The peak hour is tinted tertiary. */
@Composable
fun HourGraph(
    readsByHour: List<Int>,
    modifier: Modifier = Modifier,
) {
    val max = readsByHour.maxOrNull()?.takeIf { it > 0 } ?: return
    val peak = readsByHour.lastIndexOf(max)
    val bars = remember(readsByHour.size) { List(readsByHour.size) { Animatable(0f) } }
    LaunchedEffect(bars) {
        bars.forEachIndexed { index, bar ->
            launch {
                delay(BarStartDelayMs + index * BarStaggerMs)
                bar.animateTo(1f, tween(600, easing = BarEasing))
            }
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                stringResource(Res.string.do_by_hour),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.do_most_at, peakHourLabel(readsByHour).orEmpty()),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .padding(top = 10.dp)
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .height(54.dp)
        ) {
            readsByHour.forEachIndexed { hour, count ->
                val color = when {
                    count == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                    hour == peak -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(if (count == 0) 0.06f else count.toFloat() / max)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            scaleY = bars[hour].value
                        }
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                        .background(color)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 3.dp)
        ) {
            stringArrayResource(Res.array.do_hour_axis).forEach {
                Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
