package com.programmersbox.uiviews.presentation.onboarding

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SystemThemeMode
import com.programmersbox.datastore.ThemeColor
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.ListSetting
import com.programmersbox.kmpuiviews.presentation.components.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.SliderSetting
import com.programmersbox.kmpuiviews.presentation.components.SwitchSetting
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.presentation.components.ThemeItem
import com.programmersbox.uiviews.presentation.components.seedColor
import com.programmersbox.uiviews.presentation.settings.BlurSetting
import com.programmersbox.uiviews.presentation.settings.NavigationBarSettings
import com.programmersbox.uiviews.presentation.settings.ShareChapterSettings
import com.programmersbox.uiviews.presentation.settings.ShowDownloadSettings
import com.programmersbox.uiviews.presentation.settings.moresettings.MoreSettingsViewModel
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.rememberDrawablePainter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeContent(
    handling: NewSettingsHandling = koinInject(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Pick your theme!") },
        )

        HorizontalDivider()

        var themeSetting by handling.rememberSystemThemeMode()

        val themeText by remember {
            derivedStateOf {
                when (themeSetting) {
                    SystemThemeMode.FollowSystem -> "System"
                    SystemThemeMode.Day -> "Light"
                    SystemThemeMode.Night -> "Dark"
                    else -> "None"
                }
            }
        }

        ListSetting(
            settingTitle = { Text(stringResource(R.string.theme_choice_title)) },
            dialogIcon = { Icon(Icons.Default.SettingsBrightness, null) },
            settingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.fillMaxSize()) },
            dialogTitle = { Text(stringResource(R.string.choose_a_theme)) },
            summaryValue = { Text(themeText) },
            confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
            value = themeSetting,
            options = listOf(SystemThemeMode.FollowSystem, SystemThemeMode.Day, SystemThemeMode.Night),
            updateValue = { it, d ->
                d.value = false
                themeSetting = it
            }
        )

        var isAmoledMode by handling.rememberIsAmoledMode()

        AnimatedVisibility(
            visible = themeSetting != SystemThemeMode.Day
        ) {
            SwitchSetting(
                settingTitle = { Text(stringResource(R.string.amoled_mode)) },
                settingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.fillMaxSize()) },
                value = isAmoledMode,
                updateValue = { isAmoledMode = it }
            )
        }

        var themeColor by handling.rememberThemeColor()

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ThemeColor.entries
                .filter { it != ThemeColor.Custom }
                .forEach {
                    ThemeItem(
                        themeColor = it,
                        onClick = { themeColor = it },
                        selected = it == themeColor,
                        colorScheme = if (it == ThemeColor.Dynamic)
                            MaterialTheme.colorScheme
                        else
                            rememberDynamicColorScheme(
                                it.seedColor,
                                isDark = when (themeSetting) {
                                    SystemThemeMode.FollowSystem -> isSystemInDarkTheme()
                                    SystemThemeMode.Day -> false
                                    SystemThemeMode.Night -> true
                                    else -> isSystemInDarkTheme()
                                },
                                isAmoled = isAmoledMode
                            )
                    )
                }
        }
    }
}

@Composable
internal fun AccountContent(
    navController: NavController,
    viewModel: AccountViewModel = koinViewModel(),
    importViewModel: MoreSettingsViewModel = koinViewModel(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("Account") }
        )

        HorizontalDivider()

        if (BuildConfig.FLAVOR == "full") {
            Text(
                "Log in to save all your favorites and chapters/episodes read to the cloud so you can access them on any device!",
                modifier = Modifier.padding(16.dp)
            )

            Text(
                "No one who works on these apps get access to your data.",
                modifier = Modifier.padding(16.dp)
            )

            Crossfade(
                viewModel.accountInfo
            ) { target ->
                if (target == null) {
                    val context = LocalContext.current
                    val activity = LocalActivity.current

                    Card(
                        onClick = {
                            (activity as? ComponentActivity)?.let {
                                viewModel.signInOrOut(context, it)
                            }
                        }
                    ) {
                        ListItem(
                            headlineContent = { Text("Log in") },
                            leadingContent = { Icon(Icons.Default.AccountCircle, null) }
                        )
                    }
                } else {
                    Card {
                        ListItem(
                            headlineContent = { Text(target.displayName.orEmpty()) },
                            leadingContent = {
                                GlideImage(
                                    imageModel = { target.photoUrl },
                                    loading = { Icon(Icons.Default.AccountCircle, null) },
                                    failure = { Icon(Icons.Default.AccountCircle, null) },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(40.dp)
                                )
                            }
                        )
                    }
                }
            }
        } else {
            val context = LocalContext.current
            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { document -> document?.let { importViewModel.importFavorites(it, context) } }

            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.import_favorites)) },
                settingIcon = { Icon(Icons.Default.Favorite, null) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null
                ) { importLauncher.launch(arrayOf("application/json")) }
            )
        }

        HorizontalDivider()

        val importListLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { document -> document?.let { navController.navigate(Screen.ImportFullListScreen(it.toString())) } }

        PreferenceSetting(
            settingTitle = { Text("Import Lists") },
            settingIcon = { Icon(Icons.AutoMirrored.Filled.List, null) },
            modifier = Modifier.clickable(
                enabled = true,
                indication = ripple(),
                interactionSource = null
            ) { importListLauncher.launch(arrayOf("application/json")) }
        )
    }
}

@Composable
internal fun FinishContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .size(150.dp)
        ) {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(R.raw.successfully_done)
            )

            LottieAnimation(
                composition = composition,
                modifier = Modifier.matchParentSize()
            )
        }

        HorizontalDivider()

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "All done!",
                style = MaterialTheme.typography.headlineMedium,
            )

            Text("Remember you can change all of these settings at any time.")

            Text("Press finish to start using the app!")
        }
    }
}

@Composable
internal fun WelcomeContent(
    appLogo: AppLogo,
) {
    val appName = stringResource(R.string.app_name)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .size(150.dp)
        ) {
            Image(
                rememberDrawablePainter(appLogo.logo),
                null,
                modifier = Modifier.matchParentSize()
            )
        }

        HorizontalDivider()

        Text(
            stringResource(R.string.welcome_description, appName),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
internal fun GeneralContent() {
    val handling = koinInject<NewSettingsHandling>()
    val dataStoreHandling = koinInject<DataStoreHandling>()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = { Text("General Settings") },
        )

        HorizontalDivider()

        BlurSetting(handling = handling)

        HorizontalDivider()

        ShowDownloadSettings(handling = handling)

        ShareChapterSettings(handling = handling)

        HorizontalDivider()

        //This has a divider with it
        NavigationBarSettings(handling = handling)

        var notifyOnBoot by handling.notifyOnReboot.rememberPreference()

        SwitchSetting(
            settingTitle = { Text("Notify on Boot") },
            value = notifyOnBoot,
            updateValue = { notifyOnBoot = it },
            settingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.fillMaxSize()) }
        )

        var canCheck by dataStoreHandling
            .shouldCheck
            .asState()

        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.are_you_sure_stop_checking)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            canCheck = false
                            showDialog = false
                        }
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.no)) } }
            )
        }

        SwitchSetting(
            settingTitle = { Text(stringResource(R.string.check_for_periodic_updates)) },
            value = canCheck,
            settingIcon = { Icon(Icons.Default.Timelapse, null, modifier = Modifier.fillMaxSize()) },
            updateValue = {
                if (!it) {
                    showDialog = true
                } else {
                    canCheck = it
                }
            }
        )

        ShowWhen(canCheck) {
            var updateHourCheck by dataStoreHandling
                .updateHourCheck
                .asState()

            var sliderValue by remember(updateHourCheck) {
                mutableFloatStateOf(updateHourCheck.toFloat())
            }

            SliderSetting(
                settingTitle = { Text("Check Every $updateHourCheck hours") },
                settingSummary = { Text("How often do you want to check for updates? Default is 1 hour.") },
                sliderValue = sliderValue,
                updateValue = { sliderValue = it },
                range = 1f..24f,
                steps = 23,
                onValueChangedFinished = { updateHourCheck = sliderValue.toLong() },
                settingIcon = {
                    Icon(
                        Icons.Default.HourglassTop,
                        null,
                    )
                }
            )
        }
    }
}