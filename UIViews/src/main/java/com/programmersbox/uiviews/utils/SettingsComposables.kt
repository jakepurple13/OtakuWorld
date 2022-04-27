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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> ListSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    settingTitle: @Composable () -> Unit,
    dialogTitle: @Composable () -> Unit,
    dialogIcon: (@Composable () -> Unit)? = null,
    cancelText: (@Composable (MutableState<Boolean>) -> Unit)? = null,
    confirmText: @Composable (MutableState<Boolean>) -> Unit,
    radioButtonColors: androidx.compose.material3.RadioButtonColors = androidx.compose.material3.RadioButtonDefaults.colors(),
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
            title = dialogTitle,
            icon = dialogIcon,
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
                            androidx.compose.material3.RadioButton(
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
            confirmButton = { confirmText(dialogPopup) },
            dismissButton = cancelText?.let { { it(dialogPopup) } }
        )

    }

    PreferenceSetting(
        settingTitle = settingTitle,
        summaryValue = summaryValue,
        settingIcon = settingIcon,
        modifier = Modifier
            .clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { dialogPopup.value = true }
            .then(modifier)
    )
}

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> MultiSelectListSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: @Composable () -> Unit,
    settingSummary: (@Composable () -> Unit)? = null,
    dialogTitle: @Composable () -> Unit,
    dialogIcon: (@Composable () -> Unit)? = null,
    cancelText: (@Composable (MutableState<Boolean>) -> Unit)? = null,
    confirmText: @Composable (MutableState<Boolean>) -> Unit,
    checkboxColors: androidx.compose.material3.CheckboxColors = androidx.compose.material3.CheckboxDefaults.colors(),
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
            title = dialogTitle,
            icon = dialogIcon,
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
                            androidx.compose.material3.Checkbox(
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
            confirmButton = { confirmText(dialogPopup) },
            dismissButton = cancelText?.let { { it(dialogPopup) } }
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
    settingTitle: @Composable () -> Unit,
    summaryValue: (@Composable () -> Unit)? = null,
    endIcon: (@Composable () -> Unit)? = null
) = DefaultPreferenceLayout(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = settingTitle,
    summaryValue = summaryValue,
    content = endIcon
)

@ExperimentalMaterialApi
@Composable
fun SwitchSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: @Composable () -> Unit,
    summaryValue: (@Composable () -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    value: Boolean,
    updateValue: (Boolean) -> Unit
) {
    DefaultPreferenceLayout(
        modifier = modifier
            /*.toggleable(
                value = value,
                onValueChange = updateValue
            )*/
            .clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { updateValue(!value) },
        settingIcon = settingIcon,
        settingTitle = settingTitle,
        summaryValue = summaryValue
    ) {
        androidx.compose.material3.Switch(
            checked = value,
            onCheckedChange = updateValue,
            colors = switchColors
        )
    }
}

@ExperimentalMaterial3Api
@ExperimentalMaterialApi
@Composable
fun CheckBoxSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: @Composable () -> Unit,
    summaryValue: (@Composable () -> Unit)? = null,
    checkboxColors: androidx.compose.material3.CheckboxColors = androidx.compose.material3.CheckboxDefaults.colors(),
    value: Boolean,
    updateValue: (Boolean) -> Unit
) {
    DefaultPreferenceLayout(
        modifier = modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { updateValue(!value) },
        settingIcon = settingIcon,
        settingTitle = settingTitle,
        summaryValue = summaryValue
    ) {
        androidx.compose.material3.Checkbox(
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
    settingTitle: @Composable () -> Unit,
    settingSummary: (@Composable () -> Unit)? = null,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    colors: SliderColors = androidx.compose.material3.SliderDefaults.colors(),
    format: (Float) -> String = { it.toInt().toString() },
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
            info,
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

        Column(
            modifier = Modifier.constrainAs(info) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                start.linkTo(icon.end, margin = 10.dp)
                width = Dimension.fillToConstraints
            }
        ) {
            androidx.compose.material3.ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, textAlign = TextAlign.Start)
            ) { settingTitle() }
            settingSummary?.let {
                androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Start)) { it() }
            }
        }

        androidx.compose.material3.Slider(
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

@Composable
fun ShowMoreSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: @Composable () -> Unit,
    summaryValue: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        var showMore by remember { mutableStateOf(false) }
        DefaultPreferenceLayout(
            modifier = modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { showMore = !showMore },
            settingIcon = settingIcon,
            settingTitle = settingTitle,
            summaryValue = summaryValue
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
    settingTitle: @Composable () -> Unit,
    summaryValue: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        val (icon, text, endIcon) = createRefs()

        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(32.dp)
                .constrainAs(icon) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    centerVerticallyTo(parent)
                },
            contentAlignment = Alignment.Center
        ) { settingIcon?.invoke(this) }

        Column(
            modifier = Modifier.constrainAs(text) {
                start.linkTo(icon.end, 8.dp)
                end.linkTo(endIcon.start, 8.dp)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                centerVerticallyTo(parent)
                width = Dimension.fillToConstraints
            }
        ) {
            androidx.compose.material3.ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, textAlign = TextAlign.Start)
            ) { settingTitle() }
            summaryValue?.let {
                androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Start)) { it() }
            }
        }

        Box(
            modifier = Modifier
                .padding(8.dp)
                .constrainAs(endIcon) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    centerVerticallyTo(parent)
                }
        ) { content?.invoke() }
    }
}

@ExperimentalMaterialApi
@Composable
fun CategorySetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: @Composable () -> Unit
) {
    CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.primary
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier)
        ) {
            val (icon, text) = createRefs()

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(32.dp)
                    .constrainAs(icon) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        centerVerticallyTo(parent)
                    },
                contentAlignment = Alignment.Center
            ) { settingIcon?.invoke(this) }

            Column(
                modifier = Modifier.constrainAs(text) {
                    start.linkTo(icon.end, 8.dp)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    centerVerticallyTo(parent)
                    width = Dimension.fillToConstraints
                }
            ) {
                androidx.compose.material3.ProvideTextStyle(
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start)
                ) { settingTitle() }
            }
        }
    }
}

@Composable
fun ShowWhen(visibility: Boolean, content: @Composable () -> Unit) {
    Column(modifier = Modifier.animateContentSize()) { if (visibility) content() }
}
