package com.programmersbox.kmpuiviews.presentation.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.annexflow.constraintlayout.compose.ConstraintLayout
import tech.annexflow.constraintlayout.compose.Dimension

@Composable
fun SliderSetting(
    sliderValue: Float,
    updateValue: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    settingTitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingSummary: (@Composable () -> Unit)? = null,
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
    format: (Float) -> String = { it.toInt().toString() },
    onValueChangedFinished: (() -> Unit)? = null,
) {
    ConstraintLayout(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .then(modifier)
    ) {
        val (
            icon,
            info,
            slider,
            value,
        ) = createRefs()

        Box(
            modifier = Modifier
                .constrainAs(icon) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .padding(8.dp)
                .padding(end = 16.dp)
        ) { settingIcon?.invoke(this) }

        Column(
            modifier = Modifier.constrainAs(info) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                start.linkTo(icon.end, margin = 10.dp)
                width = Dimension.fillToConstraints
            }
        ) {
            ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, textAlign = TextAlign.Start)
            ) { settingTitle() }
            settingSummary?.let {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Start)) { it() }
            }
        }

        Slider(
            value = sliderValue,
            onValueChange = updateValue,
            onValueChangeFinished = onValueChangedFinished,
            valueRange = range,
            steps = steps,
            colors = colors,
            modifier = Modifier.constrainAs(slider) {
                top.linkTo(info.bottom)
                end.linkTo(value.start)
                start.linkTo(icon.end)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            format(sliderValue),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .constrainAs(value) {
                    end.linkTo(parent.end)
                    start.linkTo(slider.end)
                    centerVerticallyTo(slider)
                }
                .padding(horizontal = 16.dp)
        )
    }
}
