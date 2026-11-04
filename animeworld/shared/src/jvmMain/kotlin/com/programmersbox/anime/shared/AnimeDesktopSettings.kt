package com.programmersbox.anime.shared

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

    /** ffmpeg binary for video export; blank means look it up on PATH. */
    val ffmpegPath = DataStoreHandler(
        key = stringPreferencesKey("ffmpegPath"),
        defaultValue = ""
    )

    /** Whether Downloads also lists videos from [libraryFolders]-style folders outside [downloadsDirectory]. */
    val includeOtherFolders = DataStoreHandler(
        key = booleanPreferencesKey("includeOtherFolders"),
        defaultValue = false
    )

    /** Set once the user has answered the "show every video on this computer" card, so it never asks twice. */
    val libraryScanAsked = DataStoreHandler(
        key = booleanPreferencesKey("libraryScanAsked"),
        defaultValue = false
    )

    /** Folders the user added on top of [DEFAULT_LIBRARY_FOLDERS]. */
    val extraLibraryFolders = DataStoreHandler(
        key = stringSetPreferencesKey("extraLibraryFolders"),
        defaultValue = emptySet()
    )

    /** Folders (default or added) the user unchecked. */
    val disabledLibraryFolders = DataStoreHandler(
        key = stringSetPreferencesKey("disabledLibraryFolders"),
        defaultValue = emptySet()
    )

    /** Rescan the library folders periodically so new files show up without a manual rescan. */
    val watchLibraryFolders = DataStoreHandler(
        key = booleanPreferencesKey("watchLibraryFolders"),
        defaultValue = true
    )

    companion object {
        /** Well-known video folders offered by default; only the ones that exist are shown. */
        val DEFAULT_LIBRARY_FOLDERS: List<String> = System.getProperty("user.home").let { home ->
            listOf("Movies", "Videos", "Downloads", "Desktop").map { File(home, it).absolutePath }
        }
    }
}
