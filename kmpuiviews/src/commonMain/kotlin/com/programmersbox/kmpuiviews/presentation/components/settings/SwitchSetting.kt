package com.programmersbox.kmpuiviews.presentation.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


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