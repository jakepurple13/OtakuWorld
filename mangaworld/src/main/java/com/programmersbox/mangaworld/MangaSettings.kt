package com.programmersbox.mangaworld

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.programmersbox.mangasettings.ImageLoaderType
import com.programmersbox.mangasettings.MangaSettings
import com.programmersbox.mangasettings.ReaderType
import com.programmersbox.mangasettings.mangaSettings
import com.programmersbox.uiviews.datastore.GenericSerializer
import com.programmersbox.uiviews.datastore.rememberPreference
import com.programmersbox.uiviews.datastore.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream

val Context.mangaSettings: DataStore<MangaSettings> by dataStore(
    fileName = "MangaSettings",
    serializer = MangaSettingsSerializer
)

object MangaSettingsSerializer : GenericSerializer<MangaSettings, MangaSettings.Builder> {
    override val defaultValue: MangaSettings
        get() = mangaSettings {
            useNewReader = true
            pagePadding = 4
            readerType = ReaderType.List
        }
    override val parseFrom: (input: InputStream) -> MangaSettings get() = MangaSettings::parseFrom
}

class MangaSettingsHandling(context: Context) {
    private val preferences by lazy { context.mangaSettings }
    private val all: Flow<MangaSettings> get() = preferences.data

    val useNewReader = SettingInfo(
        flow = all.map { it.useNewReader },
        updateValue = { setUseNewReader(it) }
    )

    @Composable
    fun rememberUseNewReader() = preferences.rememberPreference(
        key = { it.useNewReader },
        update = { setUseNewReader(it) },
        defaultValue = true
    )

    val pagePadding = SettingInfo(
        flow = all.map { it.pagePadding },
        updateValue = { setPagePadding(it) }
    )

    val readerType = SettingInfo(
        flow = all.map { it.readerType },
        updateValue = { setReaderType(it) }
    )

    @Composable
    fun rememberReaderType() = preferences.rememberPreference(
        key = { it.readerType },
        update = { setReaderType(it) },
        defaultValue = ReaderType.List
    )

    @Composable
    fun rememberImageLoaderType() = preferences.rememberPreference(
        key = { it.imageLoaderType },
        update = { setImageLoaderType(it) },
        defaultValue = ImageLoaderType.Kamel
    )

    @Composable
    fun rememberUserGestureEnabled() = preferences.rememberPreference(
        key = { it.allowUserDrawerGesture },
        update = { setAllowUserDrawerGesture(it) },
        defaultValue = true
    )

    @Composable
    fun rememberUseFloatingReaderBottomBar() = preferences.rememberPreference(
        key = { it.useFloatingReaderBottomBar },
        update = { setUseFloatingReaderBottomBar(it) },
        defaultValue = true
    )

    inner class SettingInfo<T>(
        val flow: Flow<T>,
        private val updateValue: suspend MangaSettings.Builder.(T) -> MangaSettings.Builder,
    ) {
        suspend fun updateSetting(value: T) = preferences.update { updateValue(value) }
    }
}