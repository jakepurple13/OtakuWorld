package com.programmersbox.mangaworld

import com.programmersbox.kmpuiviews.providers.CustomListContentProvider
import com.programmersbox.kmpuiviews.providers.FavoritesContentProvider

class MangaFavoritesContentProvider : FavoritesContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}

class MangaListContentProvider : CustomListContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}