package com.programmersbox.uiviews.utils

import androidx.annotation.StringRes
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

open class SettingViewModel(
    titleValue: String? = null,
    titleIdValue: Int? = null,
    summaryValue: String? = null,
    summaryIdValue: Int? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
) {
    init {
        require(titleValue != null || titleIdValue != null)
    }

    open var title: String by mutableStateOf(titleValue.orEmpty())
    open var titleId: Int? by mutableStateOf(titleIdValue)
    open var summary: String? by mutableStateOf(summaryValue)
    open var summaryId: Int? by mutableStateOf(summaryIdValue)
    open var icon: (@Composable BoxScope.() -> Unit)? by mutableStateOf(icon)

    @Composable
    fun titleString() = titleId?.let { stringResource(it) } ?: title

    @Composable
    fun summaryString() = summaryId?.let { stringResource(it) } ?: summary
}

class ListViewModel(
    dialogTitleText: String? = null,
    @StringRes dialogTitleId: Int? = null,
    confirmText: String? = null,
    @StringRes confirmTextId: Int? = null,
    dismissText: String? = null,
    @StringRes dismissTextId: Int? = null,
    titleValue: String? = null,
    @StringRes titleIdValue: Int? = null,
    summaryValue: String? = null,
    @StringRes summaryIdValue: Int? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null
) : SettingViewModel(titleValue, titleIdValue, summaryValue, summaryIdValue, icon) {

    init {
        require(confirmText != null || confirmTextId != null)
        require(dialogTitleText != null || dialogTitleId != null)
        require(confirmText != null || confirmTextId != null)
    }

    var confirmText: String by mutableStateOf(confirmText.orEmpty())
    var confirmTextId: Int? by mutableStateOf(confirmTextId)

    var dismissText: String? by mutableStateOf(dismissText)
    var dismissTextId: Int? by mutableStateOf(dismissTextId)

    var dialogTitleText: String by mutableStateOf(dialogTitleText.orEmpty())
    var dialogTitleTextId: Int? by mutableStateOf(dialogTitleId)

    @Composable
    fun dialogTitleString() = dialogTitleTextId?.let { stringResource(it) } ?: dialogTitleText

    @Composable
    fun confirmButtonString() = confirmTextId?.let { stringResource(it) } ?: confirmText

    @Composable
    fun dismissButtonString() = dismissTextId?.let { stringResource(it) } ?: dismissText
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> ListSetting(
    modifier: Modifier = Modifier,
    viewModel: ListViewModel,
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.secondary,
        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
    ),
    value: T,
    options: List<T>,
    viewText: (T) -> String = { it.toString() },
    updateValue: (T, MutableState<Boolean>) -> Unit
) = ListSetting(
    modifier = modifier,
    value = value,
    options = options,
    viewText = viewText,
    radioButtonColors = radioButtonColors,
    updateValue = updateValue,
    settingIcon = viewModel.icon,
    settingTitle = viewModel.titleString(),
    dialogTitle = viewModel.dialogTitleString(),
    confirmText = viewModel.confirmButtonString(),
    cancelText = viewModel.dismissButtonString()
)

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> ListSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    @StringRes settingTitleId: Int,
    @StringRes dialogTitleId: Int,
    @StringRes cancelTextId: Int? = null,
    @StringRes confirmTextId: Int,
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.secondary,
        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
    ),
    value: T,
    options: List<T>,
    viewText: (T) -> String = { it.toString() },
    updateValue: (T, MutableState<Boolean>) -> Unit
) = ListSetting(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = stringResource(settingTitleId),
    dialogTitle = stringResource(dialogTitleId),
    cancelText = cancelTextId?.let { stringResource(it) },
    confirmText = stringResource(confirmTextId),
    radioButtonColors = radioButtonColors,
    value = value,
    options = options,
    viewText = viewText,
    updateValue = updateValue
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
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.secondary,
        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
    ),
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
    viewModel: ListViewModel,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colorScheme.secondary,
        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        checkmarkColor = MaterialTheme.colorScheme.surface,
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledIndeterminateColor = MaterialTheme.colorScheme.secondary.copy(alpha = ContentAlpha.disabled)
    ),
    values: List<T>,
    options: List<T>,
    viewText: (T) -> String = { it.toString() },
    updateValue: (T, Boolean) -> Unit
) = MultiSelectListSetting(
    modifier = modifier,
    values = values,
    options = options,
    viewText = viewText,
    checkboxColors = checkboxColors,
    updateValue = updateValue,
    settingIcon = viewModel.icon,
    settingTitle = viewModel.titleString(),
    dialogTitle = viewModel.dialogTitleString(),
    confirmText = viewModel.confirmButtonString(),
    cancelText = viewModel.dismissButtonString()
)

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> MultiSelectListSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    @StringRes settingTitleId: Int,
    @StringRes settingSummaryId: Int? = null,
    @StringRes dialogTitleId: Int,
    @StringRes cancelTextId: Int? = null,
    @StringRes confirmTextId: Int,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colorScheme.secondary,
        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        checkmarkColor = MaterialTheme.colorScheme.surface,
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledIndeterminateColor = MaterialTheme.colorScheme.secondary.copy(alpha = ContentAlpha.disabled)
    ),
    values: List<T>,
    options: List<T>,
    viewText: (T) -> String = { it.toString() },
    updateValue: (T, Boolean) -> Unit
) = MultiSelectListSetting(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = stringResource(settingTitleId),
    settingSummary = settingSummaryId?.let { stringResource(it) },
    dialogTitle = stringResource(dialogTitleId),
    cancelText = cancelTextId?.let { stringResource(it) },
    confirmText = stringResource(confirmTextId),
    checkboxColors = checkboxColors,
    values = values,
    options = options,
    viewText = viewText,
    updateValue = updateValue
)

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
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colorScheme.secondary,
        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        checkmarkColor = MaterialTheme.colorScheme.surface,
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledIndeterminateColor = MaterialTheme.colorScheme.secondary.copy(alpha = ContentAlpha.disabled)
    ),
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
    viewModel: SettingViewModel
) = PreferenceSetting(
    modifier = modifier,
    settingIcon = viewModel.icon,
    settingTitle = viewModel.titleString(),
    summaryValue = viewModel.summaryString()
)

@ExperimentalMaterialApi
@Composable
fun PreferenceSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    @StringRes settingTitleId: Int,
    @StringRes summaryId: Int? = null
) = PreferenceSetting(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = stringResource(settingTitleId),
    summaryValue = summaryId?.let { stringResource(it) }
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
    viewModel: SettingViewModel,
    switchColors: SwitchColors = SwitchDefaults.colors(
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
    ),
    value: Boolean,
    updateValue: (Boolean) -> Unit
) = SwitchSetting(
    modifier = modifier,
    settingIcon = viewModel.icon,
    settingTitle = viewModel.titleString(),
    summaryValue = viewModel.summaryString(),
    switchColors = switchColors,
    value = value,
    updateValue = updateValue
)

@ExperimentalMaterialApi
@Composable
fun SwitchSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    @StringRes settingTitleId: Int,
    @StringRes summaryValueId: Int? = null,
    switchColors: SwitchColors = SwitchDefaults.colors(
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
    ),
    value: Boolean,
    updateValue: (Boolean) -> Unit
) = SwitchSetting(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = stringResource(settingTitleId),
    summaryValue = summaryValueId?.let { stringResource(it) },
    switchColors = switchColors,
    value = value,
    updateValue = updateValue
)

@ExperimentalMaterialApi
@Composable
fun SwitchSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    summaryValue: String? = null,
    switchColors: SwitchColors = SwitchDefaults.colors(
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
    ),
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
fun SwitchSetting(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colorScheme.secondary,
        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        checkmarkColor = MaterialTheme.colorScheme.surface,
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledIndeterminateColor = MaterialTheme.colorScheme.secondary.copy(alpha = ContentAlpha.disabled)
    ),
    value: Boolean,
    updateValue: (Boolean) -> Unit
) = CheckBoxSetting(
    modifier = modifier,
    settingIcon = viewModel.icon,
    settingTitle = viewModel.titleString(),
    summaryValue = viewModel.summaryString(),
    checkboxColors = checkboxColors,
    value = value,
    updateValue = updateValue
)

@ExperimentalMaterialApi
@Composable
fun CheckBoxSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    @StringRes settingTitleId: Int,
    @StringRes summaryValueId: Int? = null,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colorScheme.secondary,
        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        checkmarkColor = MaterialTheme.colorScheme.surface,
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledIndeterminateColor = MaterialTheme.colorScheme.secondary.copy(alpha = ContentAlpha.disabled)
    ),
    value: Boolean,
    updateValue: (Boolean) -> Unit
) = CheckBoxSetting(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = stringResource(settingTitleId),
    summaryValue = summaryValueId?.let { stringResource(it) },
    checkboxColors = checkboxColors,
    value = value,
    updateValue = updateValue
)

@ExperimentalMaterialApi
@Composable
fun CheckBoxSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    settingTitle: String,
    summaryValue: String? = null,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colorScheme.secondary,
        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        checkmarkColor = MaterialTheme.colorScheme.surface,
        disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledIndeterminateColor = MaterialTheme.colorScheme.secondary.copy(alpha = ContentAlpha.disabled)
    ),
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
                .padding(end = 16.dp)
        )

        Text(
            stringResource(settingTitle),
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
    viewModel: SettingViewModel,
    content: @Composable () -> Unit
) = ShowMoreSetting(
    modifier = modifier,
    settingIcon = viewModel.icon,
    settingTitle = viewModel.titleString(),
    summaryValue = viewModel.summaryString(),
    content = content
)

@Composable
fun ShowMoreSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    @StringRes settingTitleId: Int,
    @StringRes summaryValueId: Int? = null,
    content: @Composable () -> Unit
) = ShowMoreSetting(
    modifier = modifier,
    settingIcon = settingIcon,
    settingTitle = stringResource(settingTitleId),
    summaryValue = summaryValueId?.let { stringResource(it) },
    content = content
)

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
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
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