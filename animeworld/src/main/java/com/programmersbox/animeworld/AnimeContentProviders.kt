package com.programmersbox.animeworld

import com.programmersbox.kmpuiviews.providers.CustomListContentProvider
import com.programmersbox.kmpuiviews.providers.FavoritesContentProvider

class AnimeFavoritesContentProvider : FavoritesContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}

class AnimeListContentProvider : CustomListContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}