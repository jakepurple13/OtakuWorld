package com.programmersbox.animeworld.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp

@ExperimentalComposeUiApi
@Composable
fun AirBar(
    progress: Float,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    fillColor: Brush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary)),
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    cornerRadius: CornerRadius = CornerRadius(x = 40.dp.value, y = 40.dp.value),
    minValue: Double = 0.0,
    maxValue: Double = 100.0,
    icon: (@Composable () -> Unit)? = null,
    topIcon: (@Composable () -> Unit)? = null,
    valueChanged: (Float) -> Unit
) = AirBarSetup(progress, modifier, isHorizontal, fillColor, null, backgroundColor, cornerRadius, minValue, maxValue, icon, topIcon, valueChanged)

@ExperimentalComposeUiApi
@Composable
fun AirBar(
    progress: Float,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    cornerRadius: CornerRadius = CornerRadius(x = 40.dp.value, y = 40.dp.value),
    minValue: Double = 0.0,
    maxValue: Double = 100.0,
    icon: (@Composable () -> Unit)? = null,
    topIcon: (@Composable () -> Unit)? = null,
    valueChanged: (Float) -> Unit
) = AirBarSetup(progress, modifier, isHorizontal, null, fillColor, backgroundColor, cornerRadius, minValue, maxValue, icon, topIcon, valueChanged)

@ExperimentalComposeUiApi
@Composable
private fun AirBarSetup(
    progress: Float,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false,
    fillColorBrush: Brush? = null,
    fillColor: Color? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    cornerRadius: CornerRadius = CornerRadius(x = 40.dp.value, y = 40.dp.value),
    minValue: Double = 0.0,
    maxValue: Double = 100.0,
    icon: (@Composable () -> Unit)? = null,
    topIcon: (@Composable () -> Unit)? = null,
    valueChanged: (Float) -> Unit
) {

    var bottomY = 0f
    var rightX = 0f

    fun reverseCalculateValues(realPercentage: Float): Float {
        val p = if (isHorizontal)
            realPercentage * rightX / 100
        else
            bottomY - realPercentage * bottomY / 100

        return String.format("%.2f", p).toFloat()
    }

    fun calculateValues(touchY: Float, touchX: Float): Float {
        val rawPercentage = if (isHorizontal) {
            String.format("%.2f", (touchX.toDouble() / rightX.toDouble()) * 100).toDouble()
        } else {
            String.format("%.2f", 100 - ((touchY.toDouble() / bottomY.toDouble()) * 100)).toDouble()
        }

        val percentage = if (rawPercentage < 0) 0.0 else if (rawPercentage > 100) 100.0 else rawPercentage

        return String.format("%.2f", ((percentage / 100) * (maxValue - minValue) + minValue)).toFloat()
    }

    Box(
        modifier = modifier
            .pointerInteropFilter { event ->
                if (!isHorizontal) {
                    when {
                        event.y in 0.0..bottomY.toDouble() -> {
                            valueChanged(calculateValues(event.y, event.x))
                            true
                        }

                        event.y > 100 -> {
                            valueChanged(calculateValues(event.y, event.x))
                            true
                        }

                        event.y < 0 -> {
                            valueChanged(calculateValues(event.y, event.x))
                            true
                        }

                        else -> false
                    }
                } else {
                    when {
                        event.x in 0.0..rightX.toDouble() -> {
                            valueChanged(calculateValues(event.y, event.x))
                            true
                        }

                        event.x > 100 -> {
                            valueChanged(calculateValues(event.y, event.x))
                            true
                        }

                        event.x < 0 -> {
                            valueChanged(calculateValues(event.y, event.x))
                            true
                        }

                        else -> false
                    }
                }
            }
            .drawBehind {
                bottomY = size.height
                rightX = size.width

                val path = Path()
                path.addRoundRect(
                    roundRect = RoundRect(
                        0F,
                        0F,
                        size.width,
                        size.height,
                        cornerRadius
                    )
                )
                drawContext.canvas.drawPath(path, Paint().apply {
                    color = backgroundColor
                    isAntiAlias = true
                })
                drawContext.canvas.clipPath(path = path, ClipOp.Intersect)
                if (fillColor != null) {
                    drawRect(
                        fillColor,
                        Offset(0f, if (isHorizontal) 0f else reverseCalculateValues(progress)),
                        Size(if (isHorizontal) reverseCalculateValues(progress) else rightX, size.height)
                    )
                } else if (fillColorBrush != null) {
                    drawRect(
                        fillColorBrush,
                        Offset(0f, if (isHorizontal) 0f else reverseCalculateValues(progress)),
                        Size(if (isHorizontal) reverseCalculateValues(progress) else rightX, size.height)
                    )
                }
            },
        contentAlignment = if (isHorizontal) Alignment.CenterStart else Alignment.Center
    ) {
        topIcon?.let {
            Box(
                modifier = (if (!isHorizontal) Modifier.padding(top = 15.dp) else Modifier.padding(start = 15.dp))
                    .align(if (!isHorizontal) Alignment.TopCenter else Alignment.CenterEnd)
            ) { it() }
        }
        icon?.let {
            Box(
                modifier = (if (!isHorizontal) Modifier.padding(bottom = 15.dp) else Modifier.padding(start = 15.dp))
                    .align(if (!isHorizontal) Alignment.BottomCenter else Alignment.CenterStart)
            ) { it() }
        }
    }
}