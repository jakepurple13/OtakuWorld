package com.programmersbox.uiviews.presentation.onboarding

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.SystemThemeMode
import com.programmersbox.uiviews.ThemeColor
import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.datastore.asState
import com.programmersbox.uiviews.presentation.Screen
import com.programmersbox.uiviews.presentation.components.ListSetting
import com.programmersbox.uiviews.presentation.components.NormalOtakuScaffold
import com.programmersbox.uiviews.presentation.components.SwitchSetting
import com.programmersbox.uiviews.presentation.components.ThemeItem
import com.programmersbox.uiviews.presentation.components.seedColor
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.programmersbox.uiviews.utils.HideSystemBarsWhileOnScreen
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.rememberDrawablePainter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    appLogo: AppLogo = koinInject(),
    navController: NavController,
    dataStoreHandling: DataStoreHandling = koinInject(),
) {
    HideSystemBarsWhileOnScreen()

    var hasSeenOnboarding by dataStoreHandling
        .hasGoneThroughOnboarding
        .asState()

    LaunchedEffect(hasSeenOnboarding) {
        if (hasSeenOnboarding) {
            navController.navigate(Screen.RecentScreen) {
                popUpTo(Screen.OnboardingScreen) {
                    inclusive = true
                }
            }
        }
    }

    val appName = stringResource(R.string.app_name)

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState {
        3 + if (BuildConfig.FLAVOR == "full") 1 else 0
    }

    NormalOtakuScaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Welcome!") },
                    subtitle = { Text(appName) }
                )
                HorizontalDivider()
            }
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    TextButton(
                        onClick = { hasSeenOnboarding = true }
                    ) { Text("Skip") }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.wrapContentHeight()
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = when {
                                pagerState.currentPage == iteration -> MaterialTheme.colorScheme.primary
                                pagerState.currentPage > iteration -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }

                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(16.dp)
                            )
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (pagerState.canScrollForward) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                hasSeenOnboarding = true
                            }
                        }
                    ) {
                        if (pagerState.canScrollForward) {
                            Text("Next")
                        } else {
                            Text("Finish!")
                        }
                    }
                }
            )
        }
    ) { padding ->

        //TODO: Add login screen
        // add account info screen
        // maybe even see if we can get the account created when

        HorizontalPager(
            state = pagerState,
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) { page ->
            when (page) {
                0 -> WelcomeContent(
                    appLogo = appLogo,
                )

                1 -> ThemeContent()

                //TODO: Maybe make an enum for the flavors?
                2 -> if (BuildConfig.FLAVOR == "full") {
                    AccountContent()
                } else {
                    FinishContent()
                }

                pagerState.pageCount -> FinishContent()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ThemeContent(
    handling: SettingsHandling = koinInject(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListItem(
            headlineContent = {
                Text(
                    "Pick your theme!"
                )
            },
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
                //TODO: For later
                .filter { it != ThemeColor.Custom && it != ThemeColor.UNRECOGNIZED }
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
private fun AccountContent(
    viewModel: AccountViewModel = koinViewModel(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Log in to save all your favorites and chapters/episodes read to the cloud so you can access them on any device!")

        Text("No one who works on these apps get access to your data.")

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

    }
}

@Composable
private fun FinishContent() {
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

        Text(
            "All done! Press finish to start using the app!",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun WelcomeContent(
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