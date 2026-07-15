package com.programmersbox.anime.shared

import androidx.datastore.preferences.core.stringPreferencesKey
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreHandler
import java.io.File

class AnimeDesktopSettings(
    appDirs: AppDirs,
) {
    val downloadsDirectory = DataStoreHandler(
        key = stringPreferencesKey("downloadsDirectory"),
        defaultValue = File("${System.getProperty("user.home")}/Downloads/AnimeWorld").absolutePath
    )
}
