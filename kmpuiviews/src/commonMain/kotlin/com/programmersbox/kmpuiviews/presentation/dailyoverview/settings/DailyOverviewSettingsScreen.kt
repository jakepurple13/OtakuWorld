package com.programmersbox.kmpuiviews.presentation.dailyoverview.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.settings.SettingsScaffold
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.cancel
import otakuworld.kmpuiviews.generated.resources.do_group_notification
import otakuworld.kmpuiviews.generated.resources.do_group_summary
import otakuworld.kmpuiviews.generated.resources.do_notification_time
import otakuworld.kmpuiviews.generated.resources.do_send_morning
import otakuworld.kmpuiviews.generated.resources.do_send_morning_summary
import otakuworld.kmpuiviews.generated.resources.do_settings_foot
import otakuworld.kmpuiviews.generated.resources.do_settings_title
import otakuworld.kmpuiviews.generated.resources.do_skip_quiet
import otakuworld.kmpuiviews.generated.resources.do_skip_quiet_summary
import otakuworld.kmpuiviews.generated.resources.do_summary_engine
import otakuworld.kmpuiviews.generated.resources.do_time
import otakuworld.kmpuiviews.generated.resources.do_time_summary
import otakuworld.kmpuiviews.generated.resources.do_use_daily_overview
import otakuworld.kmpuiviews.generated.resources.do_use_daily_overview_summary
import otakuworld.kmpuiviews.generated.resources.ok

private val GroupShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyOverviewSettingsScreen(
    viewModel: DailyOverviewSettingsViewModel = koinViewModel(),
) {
    val navActions = LocalNavActions.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val is24Hour = koinInject<DateTimeFormatHandler>().is24Time()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    // Back from the engine page or the AI Helper, the resolved label may have changed.
    RefreshOnReturn(viewModel::refresh)

    SettingsScaffold(title = stringResource(Res.string.do_settings_title)) {
        val state = ui ?: return@SettingsScaffold
        val prefs = state.prefs

        Column(Modifier.padding(horizontal = 16.dp)) {
            HeroSwitch(
                checked = prefs.enabled,
                onCheckedChange = viewModel::setEnabled,
            )

            AnimatedVisibility(
                visible = prefs.enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    if (supportsMorningNotification) {
                        GroupLabel(stringResource(Res.string.do_group_notification))
                        Group {
                            SwitchRow(
                                icon = Icons.Default.Notifications,
                                title = stringResource(Res.string.do_send_morning),
                                summary = stringResource(Res.string.do_send_morning_summary),
                                checked = prefs.notify,
                                onCheckedChange = viewModel::setNotify,
                            )
                            AnimatedVisibility(
                                visible = prefs.notify,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                Column {
                                    RowDivider()
                                    ListItem(
                                        leadingContent = { Icon(Icons.Default.Schedule, null) },
                                        headlineContent = { Text(stringResource(Res.string.do_time)) },
                                        supportingContent = { Text(stringResource(Res.string.do_time_summary)) },
                                        trailingContent = {
                                            Text(
                                                formatMinuteOfDay(prefs.minuteOfDay, is24Hour),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable(role = Role.Button) { showTimePicker = true }
                                    )
                                    RowDivider()
                                    SwitchRow(
                                        icon = Icons.Default.DoNotDisturbOn,
                                        title = stringResource(Res.string.do_skip_quiet),
                                        summary = stringResource(Res.string.do_skip_quiet_summary),
                                        checked = prefs.skipQuiet,
                                        onCheckedChange = viewModel::setSkipQuiet,
                                    )
                                }
                            }
                        }
                    }

                    GroupLabel(stringResource(Res.string.do_group_summary))
                    Group {
                        ListItem(
                            leadingContent = { Icon(Icons.Default.AutoAwesome, null) },
                            headlineContent = { Text(stringResource(Res.string.do_summary_engine)) },
                            supportingContent = { Text(state.resolvedLabel) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable(role = Role.Button) {
                                navActions.navigate(Screen.DailyOverview.Engine)
                            }
                        )
                    }

                    Text(
                        stringResource(Res.string.do_settings_foot),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)
                    )
                }
            }
        }

        if (showTimePicker) {
            val timeState = rememberTimePickerState(
                initialHour = prefs.minuteOfDay / 60,
                initialMinute = prefs.minuteOfDay % 60,
                is24Hour = is24Hour,
            )
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text(stringResource(Res.string.do_notification_time)) },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text(stringResource(Res.string.cancel)) }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.setTime(timeState.hour * 60 + timeState.minute)
                            showTimePicker = false
                        }
                    ) { Text(stringResource(Res.string.ok)) }
                },
            ) { TimePicker(timeState) }
        }
    }
}

/** "8:00 AM", or "08:00" on a 24-hour clock. */
internal fun formatMinuteOfDay(minuteOfDay: Int, is24Hour: Boolean): String {
    val hour = minuteOfDay / 60
    val minute = (minuteOfDay % 60).toString().padStart(2, '0')
    if (is24Hour) return "${hour.toString().padStart(2, '0')}:$minute"
    val hour12 = (hour % 12).let { if (it == 0) 12 else it }
    return "$hour12:$minute ${if (hour < 12) "AM" else "PM"}"
}

@Composable
private fun HeroSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val container by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    )
    val content by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    )
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = container,
        contentColor = content,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.do_use_daily_overview), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(Res.string.do_use_daily_overview_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.8f),
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    )
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) { Column(content = content) }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
