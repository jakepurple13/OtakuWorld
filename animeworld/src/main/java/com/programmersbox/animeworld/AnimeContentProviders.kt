package com.programmersbox.animeworld

import com.programmersbox.kmpuiviews.providers.CustomListContentProvider
import com.programmersbox.kmpuiviews.providers.FavoritesContentProvider
import com.programmersbox.kmpuiviews.providers.IncognitoContentProvider

class AnimeFavoritesContentProvider : FavoritesContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}

class AnimeListContentProvider : CustomListContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}

class AnimeIncognitoContentProvider : IncognitoContentProvider() {
    override val applicationId: String = BuildConfig.APPLICATION_ID
}