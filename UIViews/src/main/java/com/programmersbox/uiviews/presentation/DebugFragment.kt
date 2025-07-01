package com.programmersbox.uiviews.presentation

import android.annotation.SuppressLint
import android.text.format.DateFormat
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.datepicker.DateValidatorPointForward
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.presentation.components.settings.CheckBoxSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowMoreSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.utils.LocalCurrentSource
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.LocalSystemDateTimeFormat
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import org.koin.compose.koinInject
import java.util.Calendar

@SuppressLint("ComposeContentEmitterReturningValues")
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun DebugView() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentSourceRepository = LocalCurrentSource.current
    val genericInfo = koinInject<GenericInfo>()
    val navController = LocalNavActions.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val sourceRepo = LocalSourcesRepository.current

    val dataStoreHandling: DataStoreHandling = koinInject()

    val sources by sourceRepo.sources.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Debug Menu") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.Close, null) }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { p ->
        val moreSettings = remember { genericInfo.debugMenuItem() }
        LazyColumn(contentPadding = p) {
            /*item {
                sources.forEach {
                    PreferenceSetting(
                        settingTitle = { Text(it.name) },
                        settingIcon = { Icon(rememberDrawablePainter(drawable = it.icon), null, modifier = Modifier.fillMaxSize()) },
                        endIcon = {
                            IconButton(
                                onClick = {
                                    val uri = Uri.fromParts("package", it.packageName, null)
                                    val uninstall = Intent(Intent.ACTION_DELETE, uri)
                                    context.startActivity(uninstall)
                                }
                            ) { Icon(Icons.Default.Delete, null) }
                        },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null
                        ) { currentSourceRepository.tryEmit(it.apiService) }
                    )
                }
            }*/

            /*item {
                Button(
                    onClick = { context.startActivity(WorkInspector.getIntent(context)) }
                ) { Text("Work Inspector") }
            }*/

            item {
                var showDatePicker by remember { mutableStateOf(false) }
                var showTimePicker by remember { mutableStateOf(false) }

                val dateState = rememberDatePickerState(
                    initialSelectedDateMillis = System.currentTimeMillis(),
                    selectableDates = remember {
                        object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                return DateValidatorPointForward.now().isValid(utcTimeMillis)
                            }
                        }
                    }
                )
                val calendar = remember { Calendar.getInstance() }
                val is24HourFormat by rememberUpdatedState(DateFormat.is24HourFormat(context))
                val timeState = rememberTimePickerState(
                    initialHour = calendar[Calendar.HOUR_OF_DAY],
                    initialMinute = calendar[Calendar.MINUTE],
                    is24Hour = is24HourFormat
                )

                if (showTimePicker) {
                    TimePickerDialog(
                        onDismissRequest = { showTimePicker = false },
                        title = { Text(stringResource(id = R.string.selectTime)) },
                        dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
                        confirmButton = {
                            val dateTimeFormatter = LocalSystemDateTimeFormat.current
                            TextButton(
                                onClick = {
                                    showTimePicker = false
                                    val c = Calendar.getInstance()
                                    c.timeInMillis = dateState.selectedDateMillis ?: 0L
                                    c[Calendar.DAY_OF_YEAR] += 1
                                    c[Calendar.HOUR_OF_DAY] = timeState.hour
                                    c[Calendar.MINUTE] = timeState.minute

                                    println("Time: ${dateState.selectedDateMillis}")
                                    dateState.selectedDateMillis?.toLocalDateTime()
                                        ?.let { "Time: ${dateTimeFormatter.format(it)}" }
                                        ?.let(::println)
                                    println("Time: ${timeState.hour}")
                                    println("Time: ${timeState.minute}")
                                    c.timeInMillis.toLocalDateTime()
                                        .let { "Time: ${dateTimeFormatter.format(it)}" }
                                        .let(::println)
                                    println("Time: ${c.timeInMillis - System.currentTimeMillis()}")
                                }
                            ) { Text(stringResource(R.string.ok)) }
                        }
                    ) { TimePicker(state = timeState) }
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDatePicker = false
                                    showTimePicker = true
                                }
                            ) { Text(stringResource(R.string.ok)) }
                        }
                    ) {
                        DatePicker(
                            state = dateState,
                            title = { Text(stringResource(R.string.selectDate)) }
                        )
                    }
                }

                Button(
                    onClick = { showDatePicker = true }
                ) { Text("Date") }
            }

            item {
                Button(
                    onClick = { error("Crash") }
                ) { Text("Crash") }
            }

            item {
                Surface(
                    color = Color.Blue,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Here!") }
            }

            item {
                PreferenceSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                    settingIcon = { Icon(Icons.Default.Deck, null) }
                )

                PreferenceSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                )

                PreferenceSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                    settingIcon = { Icon(Icons.Default.Deck, null) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { println("Hello") }
                )

                HorizontalDivider()
            }

            item {
                var showMoreOptions by remember { mutableStateOf(false) }

                SwitchSetting(
                    settingTitle = { Text("Show More") },
                    settingIcon = { Icon(Icons.Default.Deck, null) },
                    value = showMoreOptions,
                    updateValue = { showMoreOptions = it }
                )

                ShowWhen(showMoreOptions) {
                    PreferenceSetting(
                        settingTitle = { Text("Title") },
                        summaryValue = { Text("Summary") },
                        settingIcon = { Icon(Icons.Default.Deck, null) }
                    )

                    PreferenceSetting(
                        settingTitle = { Text("Title") },
                        summaryValue = { Text("Summary") },
                        settingIcon = { Icon(Icons.Default.Deck, null) }
                    )

                    PreferenceSetting(
                        settingTitle = { Text("Title") },
                        summaryValue = { Text("Summary") },
                        settingIcon = { Icon(Icons.Default.Deck, null) }
                    )
                }

                HorizontalDivider()
            }

            item {
                ShowMoreSetting(
                    settingTitle = { Text("Show More") },
                    summaryValue = { Text("More Options Here") },
                    settingIcon = { Icon(Icons.Default.Deck, null) }
                ) {
                    PreferenceSetting(
                        settingTitle = { Text("Title") },
                        summaryValue = { Text("Summary") },
                        settingIcon = { Icon(Icons.Default.Deck, null) }
                    )

                    PreferenceSetting(
                        settingTitle = { Text("Title") },
                        summaryValue = { Text("Summary") },
                        settingIcon = { Icon(Icons.Default.Deck, null) }
                    )

                    PreferenceSetting(
                        settingTitle = { Text("Title") },
                        summaryValue = { Text("Summary") },
                        settingIcon = { Icon(Icons.Default.Deck, null) }
                    )
                }

                HorizontalDivider()
            }

            item {
                var value by remember { mutableStateOf(false) }

                SwitchSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                    settingIcon = { Icon(Icons.Default.Deck, null) },
                    value = value,
                    updateValue = { value = it }
                )

                SwitchSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                    value = value,
                    updateValue = { value = it }
                )

                SwitchSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                    settingIcon = { Icon(Icons.Default.Deck, null) },
                    value = value,
                    updateValue = { value = it }
                )

                HorizontalDivider()
            }

            item {
                var value by remember { mutableStateOf(false) }

                CheckBoxSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                    settingIcon = { Icon(Icons.Default.Deck, null) },
                    value = value,
                    updateValue = { value = it }
                )

                CheckBoxSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                    value = value,
                    updateValue = { value = it }
                )


                CheckBoxSetting(
                    settingTitle = { Text("Title") },
                    summaryValue = { Text("Summary") },
                    settingIcon = { Icon(Icons.Default.Deck, null) },
                    value = value,
                    updateValue = { value = it }
                )

                HorizontalDivider()
            }

            itemsIndexed(moreSettings) { index, build ->
                build()
                if (index < moreSettings.size - 1) HorizontalDivider()
            }

            item {
                var pauseComposition by remember { mutableStateOf(false) }
                var count by remember { mutableIntStateOf(0) }
                Column {
                    Row {
                        Text("Pause Composition")
                        Switch(
                            checked = pauseComposition,
                            onCheckedChange = { pauseComposition = it }
                        )
                    }

                    Text(count.toString())

                    Button(onClick = { count++ }) { Text("Count") }
                }
            }
        }
    }
}