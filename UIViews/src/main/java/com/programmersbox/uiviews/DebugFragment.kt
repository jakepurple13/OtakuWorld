package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.navigation.fragment.findNavController
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class DebugFragment : BaseBottomSheetDialogFragment() {

    private val genericInfo: GenericInfo by inject()

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalFoundationApi::class
    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        setContent {
            M3MaterialTheme(currentColorScheme) {

                /*var choice by remember { mutableStateOf(false) }

                Column {
                    SwitchSetting(settingTitle = { Text("Debug or Setting") }, value = choice, updateValue = { choice = it })

                    if(choice) DebugView() else SettingScreenTest()
                }*/
                DebugView()
            }
        }
    }

    @ExperimentalComposeUiApi
    @ExperimentalMaterial3Api
    @ExperimentalMaterialApi
    @Composable
    private fun SettingScreenTest() {
        SettingScreen(
            navController = findNavController(),
            logo = get(),
            genericInfo = genericInfo,
            activity = requireActivity(),
            fragment = this
        )
    }

    @ExperimentalComposeUiApi
    @ExperimentalMaterialApi
    @ExperimentalMaterial3Api
    @Composable
    private fun DebugView() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = { Text("Debug Menu") },
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.Close, null) }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { p ->
            val moreSettings = remember { genericInfo.debugMenuItem(context) }
            LazyColumn(contentPadding = p) {

                item {
                    val time by Observables.combineLatest(
                        updateCheckPublish.map { "Start: ${requireContext().getSystemDateTimeFormat().format(it)}" },
                        updateCheckPublishEnd.map { "End: ${requireContext().getSystemDateTimeFormat().format(it)}" }
                    )
                        .map { "${it.first}\n${it.second}" }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeAsState(
                            listOfNotNull(
                                requireContext().lastUpdateCheck
                                    ?.let { "Start: ${requireContext().getSystemDateTimeFormat().format(it)}" },
                                requireContext().lastUpdateCheckEnd
                                    ?.let { "End: ${requireContext().getSystemDateTimeFormat().format(it)}" }
                            )
                                .joinToString("\n")
                        )

                    PreferenceSetting(
                        settingTitle = { Text("Last Time Updates Were Checked") },
                        summaryValue = { Text(time) }
                    )
                }

                item {
                    var batteryPercent by remember {
                        mutableStateOf(runBlocking { context.dataStore.data.first()[BATTERY_PERCENT] ?: 20 }.toFloat())
                    }
                    SliderSetting(
                        sliderValue = batteryPercent,
                        settingIcon = { Icon(Icons.Default.BatteryAlert, null) },
                        settingTitle = { Text(stringResource(R.string.battery_alert_percentage)) },
                        settingSummary = { Text(stringResource(R.string.battery_default)) },
                        range = 1f..100f,
                        updateValue = {
                            batteryPercent = it
                            scope.launch { context.updatePref(BATTERY_PERCENT, it.toInt()) }
                        }
                    )
                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }

                item {
                    var value by remember { mutableStateOf(sourcePublish.value) }
                    ListSetting(
                        settingIcon = { Icon(Icons.Default.Deck, null) },
                        settingTitle = { Text("Current Source") },
                        dialogTitle = { Text("Choose a Source") },
                        confirmText = { TextButton(onClick = { it.value = false }) { Text("OK") } },
                        value = value,
                        options = genericInfo.sourceList(),
                        updateValue = { it, d ->
                            value = it
                            d.value = false
                        }
                    )

                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }

                item {
                    val value = remember { mutableStateListOf(sourcePublish.value) }

                    MultiSelectListSetting(
                        settingIcon = { Icon(Icons.Default.Deck, null) },
                        settingTitle = { Text("Current Source") },
                        dialogTitle = { Text("Choose a Source") },
                        confirmText = { TextButton(onClick = { it.value = false }) { Text("OK") } },
                        values = value,
                        options = genericInfo.sourceList(),
                        updateValue = { it, d -> if (d) value.add(it) else value.remove(it) }
                    )

                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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
                            indication = rememberRipple(),
                            interactionSource = remember { MutableInteractionSource() }
                        ) { println("Hello") }
                    )

                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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

                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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

                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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

                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
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

                    Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }

                itemsIndexed(moreSettings) { index, build ->
                    build()
                    if (index < moreSettings.size - 1) Divider(color = M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                }
            }
        }
    }
}