package com.programmersbox.anime.shared.subtitlegen

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.datastore.otakuDataStore
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toOkioPath
import java.io.File
import kotlin.io.path.createTempDirectory

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
        // With no previous store, leave a live one: ours dies with the test's scope, and DataStoreSettings in
        // later test classes only initializes the global when it is unset.
        otakuDataStore = previousStore ?: PreferenceDataStoreFactory.createWithPath(
            produceFile = { File(createTempDirectory("otaku-datastore").toFile(), "otaku.preferences_pb").toOkioPath() },
        )
    }
}
