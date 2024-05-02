package com.programmersbox.uiviews.utils

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
@Composable
fun <T> ListSetting(
    value: T,
    options: List<T>,
    updateValue: (T, MutableState<Boolean>) -> Unit,
    settingTitle: @Composable () -> Unit,
    dialogTitle: @Composable () -> Unit,
    confirmText: @Composable (MutableState<Boolean>) -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    dialogIcon: (@Composable () -> Unit)? = null,
    cancelText: (@Composable (MutableState<Boolean>) -> Unit)? = null,
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(),
    viewText: (T) -> String = { it.toString() },
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
                                    indication = ripple(),
                                    interactionSource = null
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
                indication = ripple(),
                interactionSource = null
            ) { dialogPopup.value = true }
            .then(modifier)
    )
}

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
fun <T> MultiSelectListSetting(
    settingTitle: @Composable () -> Unit,
    values: List<T>,
    options: List<T>,
    updateValue: (T, Boolean) -> Unit,
    dialogTitle: @Composable () -> Unit,
    confirmText: @Composable (MutableState<Boolean>) -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingSummary: (@Composable () -> Unit)? = null,
    dialogIcon: (@Composable () -> Unit)? = null,
    cancelText: (@Composable (MutableState<Boolean>) -> Unit)? = null,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(),
    viewText: (T) -> String = { it.toString() },
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
                                    indication = ripple(),
                                    interactionSource = null
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
                indication = ripple(),
                interactionSource = null
            ) { dialogPopup.value = true }
            .then(modifier)
    )
}

@Composable
fun PreferenceSetting(
    settingTitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    endIcon: (@Composable () -> Unit)? = null
) = DefaultPreferenceLayout(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = settingTitle,
    summaryValue = summaryValue,
    content = endIcon
)

@Composable
fun SwitchSetting(
    value: Boolean,
    settingTitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    updateValue: (Boolean) -> Unit,
) {
    DefaultPreferenceLayout(
        modifier = modifier
            /*.toggleable(
                value = value,
                onValueChange = updateValue
            )*/
            .clickable(
                indication = ripple(),
                interactionSource = null
            ) { updateValue(!value) },
        settingIcon = settingIcon,
        settingTitle = settingTitle,
        summaryValue = summaryValue
    ) {
        Switch(
            checked = value,
            onCheckedChange = updateValue,
            colors = switchColors
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun SwitchSettingPreview() {
    PreviewTheme {
        Column {
            SwitchSetting(
                value = true,
                updateValue = {},
                settingTitle = { Text("Title") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                summaryValue = { Text("Value") }
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun CheckBoxSetting(
    value: Boolean,
    settingTitle: @Composable () -> Unit,
    updateValue: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(),
) {
    DefaultPreferenceLayout(
        modifier = modifier.clickable(
            indication = ripple(),
            interactionSource = null
        ) { updateValue(!value) },
        settingIcon = settingIcon,
        settingTitle = settingTitle,
        summaryValue = summaryValue
    ) {
        Checkbox(
            checked = value,
            onCheckedChange = updateValue,
            colors = checkboxColors
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@LightAndDarkPreviews
@Composable
private fun CheckboxSettingPreview() {
    PreviewTheme {
        Column {
            CheckBoxSetting(
                value = true,
                updateValue = {},
                settingTitle = { Text("Title") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                summaryValue = { Text("Value") }
            )
        }
    }
}

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

@LightAndDarkPreviews
@Composable
private fun SliderSettingPreview() {
    PreviewTheme {
        Column {
            SliderSetting(
                sliderValue = 5f,
                updateValue = {},
                range = 0f..10f,
                settingTitle = { Text("Slider") },
                settingSummary = { Text("Summary") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
            )
        }
    }
}

@Composable
fun ShowMoreSetting(
    settingTitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        var showMore by remember { mutableStateOf(false) }
        DefaultPreferenceLayout(
            modifier = modifier.clickable(
                indication = ripple(),
                interactionSource = null
            ) { showMore = !showMore },
            settingIcon = settingIcon,
            settingTitle = settingTitle,
            summaryValue = summaryValue
        ) {
            Icon(
                Icons.Default.ArrowDropDown,
                null,
                modifier = Modifier.rotate(animateFloatAsState(targetValue = if (showMore) 180f else 0f, label = "").value)
            )
        }
        ShowWhen(showMore) { content() }
    }
}

@LightAndDarkPreviews
@Composable
private fun ShowMorePreview() {
    PreviewTheme {
        Column {
            ShowMoreSetting(
                settingTitle = { Text("Title") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                summaryValue = { Text("Value") }
            ) { Text("Content") }
        }
    }
}

@Composable
private fun DefaultPreferenceLayout(
    settingTitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(16.dp)
                .size(32.dp)
        ) { settingIcon?.invoke(this) }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(3f, true)
        ) {
            ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, textAlign = TextAlign.Start)
            ) { settingTitle() }
            summaryValue?.let {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Start)) { it() }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(8.dp)
                .weight(1f)
        ) { content?.invoke() }
    }
}

@LightAndDarkPreviews
@Composable
private fun DefaultPreferenceLayoutPreview() {
    PreviewTheme {
        Column {
            DefaultPreferenceLayout(
                settingTitle = { Text("Title") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                summaryValue = { Text("Value") }
            ) { Text("Content") }
        }
    }
}

@Composable
fun CategorySetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.primary
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .size(32.dp),
            ) { settingIcon?.invoke(this) }
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxWidth()
            ) {
                ProvideTextStyle(
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start)
                ) { settingTitle() }
            }
        }
    }
}

@LightAndDarkPreviews
@Composable
private fun CategoryPreview() {
    PreviewTheme {
        Column {
            CategorySetting(
                settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                settingTitle = { Text("Title") }
            )
            CategorySetting(
                settingTitle = { Text("Title") }
            )
        }
    }
}

@Composable
fun ShowWhen(visibility: Boolean, content: @Composable () -> Unit) {
    Column(modifier = Modifier.animateContentSize()) { if (visibility) content() }
}

@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@Composable
fun <T> DynamicListSetting(
    settingTitle: @Composable () -> Unit,
    value: T,
    options: () -> List<T>,
    dialogTitle: @Composable () -> Unit,
    confirmText: @Composable (MutableState<Boolean>) -> Unit,
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    dialogIcon: (@Composable () -> Unit)? = null,
    cancelText: (@Composable (MutableState<Boolean>) -> Unit)? = null,
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(),
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
                    items(options()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = ripple(),
                                    interactionSource = null
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
                indication = ripple(),
                interactionSource = null
            ) { dialogPopup.value = true }
            .then(modifier)
    )
}