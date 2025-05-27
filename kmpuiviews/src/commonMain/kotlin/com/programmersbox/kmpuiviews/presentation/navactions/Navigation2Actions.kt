package com.programmersbox.kmpuiviews.presentation.navactions

import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.customUriHandler
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navigateToDetails
import net.thauvin.erik.urlencoder.UrlEncoderUtil

class Navigation2Actions(
    private val navController: NavHostController,
    private val customUriHandler: UriHandler = customUriHandler(navController),
) : NavigationActions {
    override fun about() {
        navController.navigate(Screen.AboutScreen)
    }

    override fun details(model: KmpItemModel) {
        navController.navigateToDetails(model)
    }

    override fun details(title: String, description: String, url: String, imageUrl: String, source: String) {
        navController.navigate(
            Screen.DetailsScreen.Details(
                title = title.ifEmpty { "NA" },
                description = description.ifEmpty { "NA" },
                url = url.let { UrlEncoderUtil.encode(it) },
                imageUrl = imageUrl.let { UrlEncoderUtil.encode(it) },
                source = source
            )
        )
    }

    override fun accountInfo() {
        navController.navigate(Screen.AccountInfo)
    }

    override fun all() {
        navController.navigate(Screen.AllScreen)
    }

    override fun customList() {
        navController.navigate(Screen.CustomListScreen)
    }

    override fun customList(customList: CustomList) {
        navController.navigate(Screen.CustomListScreen.CustomListItem(customList.item.uuid))
    }

    override fun debug() {
        navController.navigate(Screen.DebugScreen)
    }

    override fun deleteFromList(uuid: String) {
        navController.navigate(Screen.CustomListScreen.DeleteFromList(uuid = uuid))
    }

    override fun downloadInstall() {
        navController.navigate(Screen.DownloadInstallScreen)
    }

    override fun extensionList() {
        navController.navigate(Screen.ExtensionListScreen)
    }

    override fun favorites() {
        navController.navigate(Screen.FavoriteScreen)
    }

    override fun general() {
        navController.navigate(Screen.GeneralSettings)
    }

    override fun globalSearch(searchText: String?) {
        navController.navigate(Screen.GlobalSearchScreen(searchText))
    }

    override fun history() {
        navController.navigate(Screen.HistoryScreen)
    }

    override fun importFullList(uri: String) {
        navController.navigate(Screen.ImportFullListScreen(uri = uri))
    }

    override fun importList(uri: String) {
        navController.navigate(Screen.ImportListScreen(uri = uri))
    }

    override fun incognito() {
        navController.navigate(Screen.IncognitoScreen)
    }

    override fun moreInfo() {
        navController.navigate(Screen.MoreInfoSettings)
    }

    override fun moreSettings() {
        navController.navigate(Screen.MoreSettings)
    }

    override fun notifications() {
        navController.navigate(Screen.NotificationScreen) {
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun notificationsSettings() {
        navController.navigate(Screen.NotificationsSettings)
    }

    override fun order() {
        navController.navigate(Screen.OrderScreen)
    }

    override fun onboarding() {
        navController.navigate(Screen.OnboardingScreen)
    }

    override fun prerelease() {
        navController.navigate(Screen.PrereleaseScreen)
    }

    override fun recent() {
        navController.navigate(Screen.RecentScreen) {
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun otherSettings() {
        navController.navigate(Screen.OtherSettings)
    }

    override fun scanQrCode() {
        navController.navigate(Screen.ScanQrCodeScreen)
    }

    override fun settings() {
        navController.navigate(Screen.Settings) {
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun webView(url: String) {
        customUriHandler.openUri(url)
    }

    override fun workerInfo() {
        navController.navigate(Screen.WorkerInfoScreen)
    }

    override fun popBackStack(route: Any, inclusive: Boolean) {
        navController.popBackStack(route.toString(), inclusive)
    }

    override fun popBackStack() {
        navController.popBackStack()
    }

    override fun <T : Any> navigate(nav: T) {
        navController.navigate(nav)
    }

    override fun onboardingToRecent() {
        navController.navigate(Screen.RecentScreen) {
            popUpTo(Screen.OnboardingScreen) {
                inclusive = true
            }
        }
    }

    override fun toOnboarding() {
        navController.clearBackStack<Screen.RecentScreen>()
        navController.navigate(Screen.OnboardingScreen) {
            popUpTo(Screen.RecentScreen) {
                inclusive = true
            }
        }
    }

    override fun clearBackStack(nav: Any?) {
        navController.clearBackStack(nav.toString())
    }
}