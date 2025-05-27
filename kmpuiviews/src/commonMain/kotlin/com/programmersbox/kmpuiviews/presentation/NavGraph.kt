package com.programmersbox.kmpuiviews.presentation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpmodels.KmpItemModel
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
import net.thauvin.erik.urlencoder.UrlEncoderUtil

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

internal class Navigation3Actions(private val navBackStack: SnapshotStateList<NavKey>) : NavigationActions {
    override fun recent() {
        navBackStack.add(Screen.RecentScreen)
    }

    override fun details(
        title: String,
        description: String,
        url: String,
        imageUrl: String,
        source: String,
    ) {
        navBackStack.add(
            Screen.DetailsScreen.Details(
                title = title,
                description = description,
                url = url,
                imageUrl = imageUrl,
                source = source
            )
        )
    }

    override fun details(model: KmpItemModel) {
        navBackStack.add(
            Screen.DetailsScreen.Details(
                title = model.title.ifEmpty { "NA" },
                description = model.description.ifEmpty { "NA" },
                url = model.url.let { UrlEncoderUtil.encode(it) },
                imageUrl = model.imageUrl.let { UrlEncoderUtil.encode(it) },
                source = model.source.serviceName
            )
        )
    }

    override fun onboarding() {
        navBackStack.add(Screen.OnboardingScreen)
    }

    override fun all() {
        navBackStack.add(Screen.AllScreen)
    }

    override fun scanQrCode() {
        navBackStack.add(Screen.ScanQrCodeScreen)
    }

    override fun webView(url: String) {
        navBackStack.add(Screen.WebViewScreen(url))
    }

    override fun incognito() {
        navBackStack.add(Screen.IncognitoScreen)
    }

    override fun extensionList() {
        navBackStack.add(Screen.ExtensionListScreen)
    }

    override fun settings() {
        navBackStack.add(Screen.Settings)
    }

    override fun globalSearch(searchText: String?) {
        navBackStack.add(Screen.GlobalSearchScreen(searchText))
    }

    override fun customList() {
        navBackStack.add(Screen.CustomListScreen)
    }

    override fun history() {
        navBackStack.add(Screen.HistoryScreen)
    }

    override fun favorites() {
        navBackStack.add(Screen.FavoriteScreen)
    }

    override fun notifications() {
        navBackStack.add(Screen.NotificationScreen)
    }

    override fun debug() {
        navBackStack.add(Screen.DebugScreen)
    }

    override fun downloadInstall() {
        navBackStack.add(Screen.DownloadInstallScreen)
    }

    override fun order() {
        navBackStack.add(Screen.OrderScreen)
    }

    override fun general() {
        navBackStack.add(Screen.GeneralSettings)
    }

    override fun moreInfo() {
        navBackStack.add(Screen.MoreInfoSettings)
    }

    override fun moreSettings() {
        navBackStack.add(Screen.MoreSettings)
    }

    override fun accountInfo() {
        navBackStack.add(Screen.AccountInfo)
    }

    override fun prerelease() {
        navBackStack.add(Screen.PrereleaseScreen)
    }

    override fun notificationsSettings() {
        navBackStack.add(Screen.NotificationsSettings)
    }

    override fun otherSettings() {
        navBackStack.add(Screen.OtherSettings)
    }

    override fun workerInfo() {
        navBackStack.add(Screen.WorkerInfoScreen)
    }

    override fun about() {
        navBackStack.add(Screen.AboutScreen)
    }

    override fun deleteFromList(uuid: String) {
        navBackStack.add(Screen.CustomListScreen.DeleteFromList(uuid = uuid))
    }

    override fun importList(uri: String) {
        navBackStack.add(Screen.ImportListScreen(uri = uri))
    }

    override fun importFullList(uri: String) {
        navBackStack.add(Screen.ImportFullListScreen(uri = uri))
    }

    override fun popBackStack() {
        navBackStack.removeLastOrNull()
    }

    override fun popBackStack(route: Any, inclusive: Boolean) {
        val index = navBackStack.indexOfLast { it == route }
        navBackStack.removeRange(index, navBackStack.size)
    }

    override fun navigate(nav: Any) {
        when (nav) {
            is NavKey -> navBackStack.add(nav)
        }
    }

    override fun clearBackStack(nav: Any?) {
        navBackStack.clear()
        if (nav is NavKey) navBackStack.add(nav)
    }
}