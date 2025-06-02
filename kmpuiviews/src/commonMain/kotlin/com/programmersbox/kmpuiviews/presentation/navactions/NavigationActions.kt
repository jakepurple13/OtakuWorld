package com.programmersbox.kmpuiviews.presentation.navactions

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.kmpmodels.KmpItemModel


interface NavigationActions {
    fun recent()
    fun details(
        title: String,
        description: String,
        url: String,
        imageUrl: String,
        source: String,
    )

    fun details(model: KmpItemModel)

    fun onboarding()
    fun all()
    fun scanQrCode()
    fun webView(url: String)
    fun incognito()
    fun extensionList()
    fun settings()
    fun globalSearch(searchText: String? = null)
    fun customList()
    fun customList(customList: CustomList)
    fun history()
    fun favorites()
    fun notifications()
    fun debug()
    fun downloadInstall()
    fun order()
    fun general()
    fun moreInfo()
    fun moreSettings()
    fun accountInfo()
    fun prerelease()
    fun notificationsSettings()
    fun otherSettings()
    fun workerInfo()
    fun about()
    fun deleteFromList(uuid: String)
    fun importList(uri: String)
    fun importFullList(uri: String)
    fun popBackStack()
    fun popBackStack(route: Any, inclusive: Boolean)
    fun <T : Any> navigate(nav: T)
    fun <T : Any> clearBackStack(nav: T)

    fun <T : Any> homeScreenNavigate(nav: T)
    fun onboardingToRecent()
    fun toOnboarding()
}