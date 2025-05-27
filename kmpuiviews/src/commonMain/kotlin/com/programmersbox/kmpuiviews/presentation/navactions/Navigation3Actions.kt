package com.programmersbox.kmpuiviews.presentation.navactions

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.presentation.Screen
import net.thauvin.erik.urlencoder.UrlEncoderUtil

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

    override fun customList(customList: CustomList) {
        navBackStack.add(Screen.CustomListScreen.CustomListItem(customList.item.uuid))
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

    override fun <T : Any> navigate(nav: T) {
        when (nav) {
            is NavKey -> navBackStack.add(nav)
        }
    }

    override fun onboardingToRecent() {

    }

    override fun toOnboarding() {

    }

    override fun <T : Any> homeScreenNavigate(nav: T) {
        navigate(nav)
    }

    override fun clearBackStack(nav: Any?) {
        if (nav is NavKey) {
            navBackStack.add(nav)
            val size = navBackStack.size - 1
            repeat(size) {
                if (navBackStack.size > 1) {
                    navBackStack.removeLastOrNull()
                }
            }
            navBackStack.add(nav)
        }
    }
}