package com.programmersbox.anime.shared.subtitlegen

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.datastore.otakuDataStore
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toOkioPath
import java.io.File

/** Points the global [otakuDataStore] at a fresh DataStore in [root] for one test; [close] restores the previous one. */
class TestDesktopSettings(root: File, scope: CoroutineScope) : AutoCloseable {
    private val previousStore = runCatching { otakuDataStore }.getOrNull()
    val settings: AnimeDesktopSettings

    init {
        otakuDataStore = PreferenceDataStoreFactory.createWithPath(
            scope = scope,
            produceFile = { File(root, "settings.preferences_pb").toOkioPath() },
        )
        settings = AnimeDesktopSettings(AppDirs { appName = "AnimeWorldTest"; appAuthor = "test" })
    }

    override fun close() {
        if (previousStore != null) otakuDataStore = previousStore
    }
}
