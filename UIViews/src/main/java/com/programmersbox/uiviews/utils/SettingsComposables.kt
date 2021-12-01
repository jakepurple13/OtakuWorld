package com.programmersbox.uiviews.utils

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun defaultSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.secondary,
    checkedTrackColor = MaterialTheme.colorScheme.secondary,
    uncheckedThumbColor = MaterialTheme.colorScheme.inverseSurface,
    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface,
    disabledCheckedThumbColor = MaterialTheme.colorScheme.secondary
        .copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.inverseSurface),
    disabledCheckedTrackColor = MaterialTheme.colorScheme.secondary
        .copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.inverseSurface),
    disabledUncheckedThumbColor = MaterialTheme.colorScheme.inverseSurface
        .copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.inverseSurface),
    disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface
        .copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.inverseSurface)
)

@Composable
fun defaultRadioButtonColors() = RadioButtonDefaults.colors(
    selectedColor = MaterialTheme.colorScheme.secondary,
    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
)

@Composable
fun defaultCheckBoxColors() = CheckboxDefaults.colors(
    checkedColor = MaterialTheme.colorScheme.secondary,
    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    checkmarkColor = MaterialTheme.colorScheme.surface,
    disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
    disabledIndeterminateColor = MaterialTheme.colorScheme.secondary.copy(alpha = ContentAlpha.disabled)
)

@Composable
fun defaultSliderColors() = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.surface),
    activeTrackColor = MaterialTheme.colorScheme.primary,
    disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SliderDefaults.DisabledActiveTrackAlpha),
    activeTickColor = contentColorFor(MaterialTheme.colorScheme.primary).copy(alpha = SliderDefaults.TickAlpha)
)

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> ListSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    dialogTitle: String,
    cancelText: String? = null,
    confirmText: String,
    radioButtonColors: RadioButtonColors = defaultRadioButtonColors(),
    value: T,
    options: List<T>,
    viewText: (T) -> String = { it.toString() },
    updateValue: (T, MutableState<Boolean>) -> Unit
) {
    val dialogPopup = remember { mutableStateOf(false) }

    if (dialogPopup.value) {

        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { dialogPopup.value = false },
            title = { Text(dialogTitle) },
            text = {
                LazyColumn {
                    items(options) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = rememberRipple(),
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { updateValue(it, dialogPopup) }
                                .border(0.dp, Color.Transparent, RoundedCornerShape(20.dp))
                        ) {
                            RadioButton(
                                selected = it == value,
                                onClick = { updateValue(it, dialogPopup) },
                                modifier = Modifier.padding(8.dp),
                                colors = radioButtonColors
                            )
                            Text(
                                viewText(it),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialogPopup.value = false }) { Text(confirmText) } },
            dismissButton = cancelText?.let { { TextButton(onClick = { dialogPopup.value = false }) { Text(it) } } }
        )

    }

    PreferenceSetting(
        settingTitle = settingTitle,
        summaryValue = viewText(value),
        settingIcon = settingIcon,
        modifier = Modifier
            .clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { dialogPopup.value = true }
            .then(modifier)
    )
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> MultiSelectListSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    settingSummary: String? = null,
    dialogTitle: String,
    cancelText: String? = null,
    confirmText: String,
    checkboxColors: CheckboxColors = defaultCheckBoxColors(),
    values: List<T>,
    options: List<T>,
    viewText: (T) -> String = { it.toString() },
    updateValue: (T, Boolean) -> Unit
) {
    val dialogPopup = remember { mutableStateOf(false) }

    if (dialogPopup.value) {

        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { dialogPopup.value = false },
            title = { Text(dialogTitle) },
            text = {
                LazyColumn {
                    items(options) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = rememberRipple(),
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { updateValue(it, it !in values) }
                                .border(0.dp, Color.Transparent, RoundedCornerShape(20.dp))
                        ) {
                            Checkbox(
                                checked = it in values,
                                onCheckedChange = { b -> updateValue(it, b) },
                                colors = checkboxColors,
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                viewText(it),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialogPopup.value = false }) { Text(confirmText) } },
            dismissButton = cancelText?.let { { TextButton(onClick = { dialogPopup.value = false }) { Text(it) } } }
        )

    }

    PreferenceSetting(
        settingTitle = settingTitle,
        summaryValue = settingSummary,
        settingIcon = settingIcon,
        modifier = Modifier
            .clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { dialogPopup.value = true }
            .then(modifier)
    )
}

@ExperimentalMaterialApi
@Composable
fun PreferenceSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    summaryValue: String? = null,
    onClick: (() -> Unit)? = null,
    endIcon: @Composable () -> Unit
) = DefaultPreferenceLayout(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = settingTitle,
    summaryValue = summaryValue,
    onClick = onClick,
    content = endIcon
)

@ExperimentalMaterialApi
@Composable
fun PreferenceSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    summaryValue: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .requiredWidth(32.dp)
        ) { settingIcon?.invoke(this) }
        Column(
            modifier = Modifier
                .padding(8.dp)
                .padding(start = 8.dp)
        ) {
            Text(
                settingTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            summaryValue?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun SwitchSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    summaryValue: String? = null,
    switchColors: SwitchColors = defaultSwitchColors(),
    value: Boolean,
    updateValue: (Boolean) -> Unit
) {
    DefaultPreferenceLayout(
        modifier = modifier,
        settingIcon = settingIcon,
        settingTitle = settingTitle,
        summaryValue = summaryValue,
        onClick = { updateValue(!value) }
    ) {
        Switch(
            checked = value,
            onCheckedChange = updateValue,
            colors = switchColors
        )
    }
}

@ExperimentalMaterialApi
@Composable
fun CheckBoxSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    summaryValue: String? = null,
    checkboxColors: CheckboxColors = defaultCheckBoxColors(),
    value: Boolean,
    updateValue: (Boolean) -> Unit
) {
    DefaultPreferenceLayout(
        modifier = modifier,
        settingIcon = settingIcon,
        settingTitle = settingTitle,
        summaryValue = summaryValue,
        onClick = { updateValue(!value) }
    ) {
        Checkbox(
            checked = value,
            onCheckedChange = updateValue,
            colors = checkboxColors
        )
    }
}

@Composable
fun SliderSetting(
    modifier: Modifier = Modifier,
    sliderValue: Float,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    settingSummary: String? = null,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    colors: SliderColors = defaultSliderColors(),
    onValueChangedFinished: (() -> Unit)? = null,
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

        Text(
            settingTitle,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                start.linkTo(icon.end, margin = 10.dp)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            settingSummary.orEmpty(),
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
            onValueChangeFinished = onValueChangedFinished,
            valueRange = range,
            steps = steps,
            colors = colors,
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

@Composable
fun ShowMoreSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    summaryValue: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        var showMore by remember { mutableStateOf(false) }
        DefaultPreferenceLayout(
            modifier = modifier,
            settingIcon = settingIcon,
            settingTitle = settingTitle,
            summaryValue = summaryValue,
            onClick = { showMore = !showMore }
        ) {
            Icon(
                Icons.Default.ArrowDropDown,
                null,
                modifier = Modifier.rotate(animateFloatAsState(targetValue = if (showMore) 180f else 0f).value)
            )
        }
        ShowWhen(showMore) { content() }
    }
}

@Composable
private fun DefaultPreferenceLayout(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    summaryValue: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                onClick?.let { c ->
                    it.clickable(
                        indication = rememberRipple(),
                        interactionSource = remember { MutableInteractionSource() }
                    ) { c() }
                } ?: it
            }
            .padding(8.dp)
            .then(modifier)
    ) {
        val (icon, text, endIcon) = createRefs()

        Box(
            modifier = Modifier
                .padding(8.dp)
                .requiredWidth(32.dp)
                .constrainAs(icon) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
        ) { settingIcon?.invoke(this) }

        Column(
            modifier = Modifier
                .padding(8.dp)
                .constrainAs(text) {
                    start.linkTo(icon.end, 8.dp)
                    end.linkTo(endIcon.start, 8.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                }
        ) {
            Text(
                settingTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
            summaryValue?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(8.dp)
                .constrainAs(endIcon) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
        ) { content() }
    }
}

@Composable
fun ShowWhen(visibility: Boolean, content: @Composable () -> Unit) {
    Column(modifier = Modifier.animateContentSize()) { if (visibility) content() }
}