package com.programmersbox.kmpuiviews.presentation.navactions

import androidx.navigation.NavController
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navigateToDetails
import net.thauvin.erik.urlencoder.UrlEncoderUtil

class Navigation2Actions(
    private val navController: NavController,
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
        TODO("Not yet implemented")
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
        navController.navigate(Screen.NotificationScreen)
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
        navController.navigate(Screen.RecentScreen)
    }

    override fun otherSettings() {
        navController.navigate(Screen.OtherSettings)
    }

    override fun scanQrCode() {
        navController.navigate(Screen.ScanQrCodeScreen)
    }

    override fun settings() {
        navController.navigate(Screen.Settings)
    }

    override fun webView(url: String) {
        navController.navigate(Screen.WebViewScreen(url))
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

    override fun navigate(nav: Any) {
        navController.navigate(nav.toString())
    }

    override fun clearBackStack(nav: Any?) {
        navController.clearBackStack(nav.toString())
    }
}