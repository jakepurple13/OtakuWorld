package com.programmersbox.mangasettings

import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import com.programmersbox.datastore.GenericSerializer
import com.programmersbox.datastore.ProtoStoreHandler
import com.programmersbox.datastore.mangasettings.ImageLoaderType
import com.programmersbox.datastore.mangasettings.MangaSettings
import com.programmersbox.datastore.mangasettings.ReaderType
import com.programmersbox.datastore.rememberPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.BufferedSource

/*val Context.mangaSettings: DataStore<MangaSettings> by dataStore(
    fileName = "MangaSettings",
    serializer = MangaSettingsSerializer
)*/

object MangaNewSettingsSerializer : GenericSerializer<MangaSettings> {
    override val defaultValue: MangaSettings
        get() = MangaSettings(
            useNewReader = true,
            pagePadding = 4,
            readerType = ReaderType.List,
            imageLoaderType = ImageLoaderType.Kamel,
            useFlipPager = false,
            allowUserDrawerGesture = false,
            useFloatingReaderBottomBar = false,
            hasMigrated = false,
        )
    override val parseFrom: (input: BufferedSource) -> MangaSettings get() = MangaSettings.ADAPTER::decode
    override fun encode(t: MangaSettings): ByteArray = t.encode()
}

class MangaNewSettingsHandling(
    val preferences: DataStore<MangaSettings>,
) {
    private val all: Flow<MangaSettings> get() = preferences.data

    val useNewReader = SettingInfo(
        flow = all.map { it.useNewReader },
        updateValue = { copy(useNewReader = it) }
    )

    @Composable
    fun rememberUseNewReader() = preferences.rememberPreference(
        key = { it.useNewReader },
        update = { copy(useNewReader = it) },
        defaultValue = true
    )

    val pagePadding = SettingInfo(
        flow = all.map { it.pagePadding },
        updateValue = { copy(pagePadding = it) }
    )

    val readerType = SettingInfo(
        flow = all.map { it.readerType },
        updateValue = { copy(readerType = it) }
    )

    @Composable
    fun rememberReaderType() = preferences.rememberPreference(
        key = { it.readerType },
        update = { copy(readerType = it) },
        defaultValue = ReaderType.List
    )

    @Composable
    fun rememberImageLoaderType() = preferences.rememberPreference(
        key = { it.imageLoaderType },
        update = { copy(imageLoaderType = it) },
        defaultValue = ImageLoaderType.Kamel
    )

    @Composable
    fun rememberUserGestureEnabled() = preferences.rememberPreference(
        key = { it.allowUserDrawerGesture },
        update = { copy(allowUserDrawerGesture = it) },
        defaultValue = true
    )

    @Composable
    fun rememberUseFloatingReaderBottomBar() = preferences.rememberPreference(
        key = { it.useFloatingReaderBottomBar },
        update = { copy(useFloatingReaderBottomBar = it) },
        defaultValue = true
    )

    val hasMigrated = ProtoStoreHandler(
        preferences = preferences,
        key = { it.hasMigrated },
        update = { copy(hasMigrated = it) },
        defaultValue = false
    )

    inner class SettingInfo<T>(
        val flow: Flow<T>,
        private val updateValue: suspend MangaSettings.(T) -> MangaSettings,
    ) {
        suspend fun updateSetting(value: T) = preferences.updateData { it.updateValue(value) }
    }
}