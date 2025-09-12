package com.programmersbox.otakuworld

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey

class DataStoreHandling(
    private val context: Context,
) {
    val lastTimeFavoritesSynced = DataStoreHandler(
        key = longPreferencesKey("lastTimeFavoritesSynced"),
        defaultValue = 0,
        context = context
    )
}