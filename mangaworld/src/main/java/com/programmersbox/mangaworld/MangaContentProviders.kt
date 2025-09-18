package com.programmersbox.mangaworld

import com.programmersbox.kmpuiviews.providers.CustomListContentProvider
import com.programmersbox.kmpuiviews.providers.FavoritesContentProvider
import com.programmersbox.kmpuiviews.providers.IncognitoContentProvider

class MangaFavoritesContentProvider : FavoritesContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}

class MangaListContentProvider : CustomListContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}

class MangaIncognitoContentProvider : IncognitoContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}