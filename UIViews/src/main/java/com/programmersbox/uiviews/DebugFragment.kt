package com.programmersbox.uiviews

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.programmersbox.uiviews.utils.*


@SuppressLint("ComposeContentEmitterReturningValues")
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
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
            item {
                sources.forEach {
                    PreferenceSetting(
                        settingTitle = { Text(it.name) },
                        settingIcon = { Icon(rememberDrawablePainter(drawable = it.icon), null, modifier = Modifier.fillMaxSize()) },
                        endIcon = {
                            IconButton(
                                onClick = {
                                    /*context.packageManager.packageInstaller.uninstall(
                                        it.packageName,
                                        PendingIntent.getActivity(context, 0, activity.intent, PendingIntent.FLAG_IMMUTABLE).intentSender
                                    )*/
                                    val uri = Uri.fromParts("package", it.packageName, null)
                                    val uninstall = Intent(Intent.ACTION_DELETE, uri)
                                    context.startActivity(uninstall)
                                }
                            ) { Icon(Icons.Default.Delete, null) }
                        },
                        modifier = Modifier.clickable(
                            indication = rememberRipple(),
                            interactionSource = remember { MutableInteractionSource() }
                        ) { currentSourceRepository.tryEmit(it.apiService) }
                    )
                }
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
                        indication = rememberRipple(),
                        interactionSource = remember { MutableInteractionSource() }
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