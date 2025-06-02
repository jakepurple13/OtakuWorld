package com.programmersbox.kmpuiviews.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.presentation.about.AboutLibrariesScreen
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreen
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScreen
import com.programmersbox.kmpuiviews.presentation.settings.SettingScreen
import com.programmersbox.kmpuiviews.presentation.settings.general.GeneralSettings
import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoScreen
import com.programmersbox.kmpuiviews.presentation.settings.lists.deletefromlist.DeleteFromListScreen
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsScreen
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettings
import com.programmersbox.kmpuiviews.presentation.settings.player.PlaySettings
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.ScanQrCode
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoScreen
import com.programmersbox.kmpuiviews.presentation.webview.WebViewScreen
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.animatedScopeComposable

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
fun NavGraphBuilder.navGraph(
    customPreferences: ComposeSettingsDsl,
    genericInfo: KmpGenericInfo,
    navController: NavHostController,
) {
    dialog<Screen.ScanQrCodeScreen> { ScanQrCode() }
    composable<Screen.OnboardingScreen> {
        OnboardingScreen(
            navController = LocalNavActions.current,
            customPreferences = customPreferences,
            navigationBarSettings = {},
            accountContent = {}
        )
    }

    composable<Screen.WebViewScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        WebViewScreen(
            url = it.toRoute<Screen.WebViewScreen>().url
        )
    }

    composable<Screen.IncognitoScreen>(
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
    ) {
        IncognitoScreen()
    }

    settings(customPreferences, genericInfo, navController) { with(genericInfo) { settingsNavSetup() } }

    with(genericInfo) { globalNavSetup() }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
private fun NavGraphBuilder.settings(
    customPreferences: ComposeSettingsDsl,
    genericInfo: KmpGenericInfo,
    navController: NavHostController,
    additionalSettings: NavGraphBuilder.() -> Unit,
) {
    navigation<Screen.Settings>(
        startDestination = Screen.SettingsScreen,
    ) {
        composable<Screen.SettingsScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            SettingScreen(
                composeSettingsDsl = customPreferences,
                notificationClick = { navController.navigate(Screen.NotificationScreen) { launchSingleTop = true } },
                favoritesClick = { navController.navigate(Screen.FavoriteScreen) { launchSingleTop = true } },
                historyClick = { navController.navigate(Screen.HistoryScreen) { launchSingleTop = true } },
                globalSearchClick = { navController.navigate(Screen.GlobalSearchScreen()) { launchSingleTop = true } },
                listClick = { navController.navigate(Screen.CustomListScreen) { launchSingleTop = true } },
                extensionClick = { navController.navigate(Screen.ExtensionListScreen) { launchSingleTop = true } },
                notificationSettingsClick = { navController.navigate(Screen.NotificationsSettings) },
                generalClick = { navController.navigate(Screen.GeneralSettings) },
                otherClick = { navController.navigate(Screen.OtherSettings) },
                moreInfoClick = { navController.navigate(Screen.MoreInfoSettings) },
                moreSettingsClick = { navController.navigate(Screen.MoreSettings) },
                geminiClick = { /*navController.navigate(Screen.GeminiScreen)*/ },
                sourcesOrderClick = { navController.navigate(Screen.OrderScreen) },
                appDownloadsClick = { navController.navigate(Screen.DownloadInstallScreen) },
                scanQrCode = { navController.navigate(Screen.ScanQrCodeScreen) },
                onDebugBuild = {},
                accountSettings = {}
            )
        }

        composable<Screen.WorkerInfoScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) { WorkerInfoScreen() }


        composable<Screen.NotificationsSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            NotificationSettings()
        }

        composable<Screen.GeneralSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            GeneralSettings(customPreferences.generalSettings)
        }

        composable<Screen.OtherSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            PlaySettings(customPreferences.playerSettings)
        }

        composable<Screen.MoreSettings>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End) },
        ) {
            MoreSettingsScreen()
        }

        composable<Screen.AboutScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            AboutLibrariesScreen()
        }

        dialog<Screen.CustomListScreen.DeleteFromList> {
            DeleteFromListScreen(
                deleteFromList = it.toRoute()
            )
        }

        animatedScopeComposable<Screen.NotificationScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            NotificationScreen()
        }

        additionalSettings()
    }
}