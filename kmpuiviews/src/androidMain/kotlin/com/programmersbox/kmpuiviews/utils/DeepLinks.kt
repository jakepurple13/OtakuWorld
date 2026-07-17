package com.programmersbox.kmpuiviews.utils

import com.programmersbox.kmpuiviews.DeepLinkRoute
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.presentation.Screen

class DeepLinks(
    genericInfo: PlatformGenericInfo,
) {
    val details by lazy {
        DeepLinkRoute(
            serializer = Screen.DetailsScreen.Details.serializer(),
            schemeAndHost = genericInfo.deepLinkUri
        )
    }

    val notification by lazy {
        DeepLinkRoute(
            serializer = Screen.NotificationScreen.serializer(),
            schemeAndHost = genericInfo.deepLinkUri
        )
    }

    val settings by lazy {
        DeepLinkRoute(
            serializer = Screen.SettingsScreen.serializer(),
            schemeAndHost = genericInfo.deepLinkUri
        )
    }

    val deepLinkMatching = listOf(
        details.toMatcher(),
        notification.toMatcher(),
        settings.toMatcher()
    )
}