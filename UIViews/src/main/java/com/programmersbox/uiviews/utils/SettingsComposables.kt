package com.programmersbox.uiviews.utils

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun SliderSetting(
    modifier: Modifier = Modifier,
    sliderValue: Float,
    settingIcon: ImageVector,
    @StringRes settingTitle: Int,
    @StringRes settingSummary: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    updateValue: (Float) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .then(modifier)
    ) {
        val (
            icon,
            title,
            summary,
            slider,
            value
        ) = createRefs()

        Icon(
            settingIcon,
            null,
            modifier = Modifier
                .constrainAs(icon) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .padding(8.dp)
        )

        Text(
            stringResource(settingTitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                start.linkTo(icon.end, margin = 10.dp)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            stringResource(settingSummary),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            modifier = Modifier.constrainAs(summary) {
                top.linkTo(title.bottom)
                end.linkTo(parent.end)
                start.linkTo(icon.end, margin = 10.dp)
                width = Dimension.fillToConstraints
            }
        )

        Slider(
            value = sliderValue,
            onValueChange = updateValue,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
                    .compositeOver(MaterialTheme.colorScheme.surface),
                activeTrackColor = MaterialTheme.colorScheme.primary,
                disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SliderDefaults.DisabledActiveTrackAlpha),
                activeTickColor = contentColorFor(MaterialTheme.colorScheme.primary)
                    .copy(alpha = SliderDefaults.TickAlpha)
            ),
            modifier = Modifier.constrainAs(slider) {
                top.linkTo(summary.bottom)
                end.linkTo(value.start)
                start.linkTo(icon.end)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            sliderValue.toInt().toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.constrainAs(value) {
                end.linkTo(parent.end)
                start.linkTo(slider.end)
                centerVerticallyTo(slider)
            }
        )

    }
}