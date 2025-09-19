package com.programmersbox.novelworld

import com.programmersbox.kmpuiviews.providers.CustomListContentProvider
import com.programmersbox.kmpuiviews.providers.FavoritesContentProvider
import com.programmersbox.kmpuiviews.providers.IncognitoContentProvider

class NovelFavoritesContentProvider : FavoritesContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}

class NovelListContentProvider : CustomListContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}

class NovelIncognitoContentProvider : IncognitoContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}