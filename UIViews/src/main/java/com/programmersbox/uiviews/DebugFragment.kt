package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.excludeFromSystemGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.fragment.findNavController
import com.programmersbox.uiviews.utils.*
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
        )
    }

    @ExperimentalComposeUiApi
    @ExperimentalMaterialApi
    @ExperimentalMaterial3Api
    @Composable
    private fun DebugView() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val topAppBarScrollState = rememberTopAppBarScrollState()
        val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarScrollState) }

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

                    Surface(
                        color = Color.Blue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .excludeFromSystemGesture()
                    ) {
                        Text("Here!")
                    }

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

                    Divider()
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

                    Divider()
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

                    Divider()
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

                    Divider()
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

                    Divider()
                }

                itemsIndexed(moreSettings) { index, build ->
                    build()
                    if (index < moreSettings.size - 1) Divider()
                }
            }
        }
    }
}