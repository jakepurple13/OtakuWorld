package com.programmersbox.uiviews

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.uiviews.utils.CheckBoxSetting
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalCurrentSource
import com.programmersbox.uiviews.utils.LocalGenericInfo
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.LocalSourcesRepository
import com.programmersbox.uiviews.utils.PreferenceSetting
import com.programmersbox.uiviews.utils.ShowMoreSetting
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.SwitchSetting


@SuppressLint("ComposeContentEmitterReturningValues")
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@Composable
fun DebugView() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentSourceRepository = LocalCurrentSource.current
    val genericInfo = LocalGenericInfo.current
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val sourceRepo = LocalSourcesRepository.current

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
        val moreSettings = remember { genericInfo.debugMenuItem(context) }
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
        }
    }
}