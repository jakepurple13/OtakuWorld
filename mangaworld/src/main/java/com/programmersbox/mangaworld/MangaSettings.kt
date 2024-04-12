package com.programmersbox.mangaworld

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.programmersbox.mangasettings.MangaSettings
import com.programmersbox.mangasettings.PlayingMiddleAction
import com.programmersbox.mangasettings.PlayingStartAction
import com.programmersbox.mangasettings.mangaSettings
import com.programmersbox.uiviews.utils.GenericSerializer
import com.programmersbox.uiviews.utils.rememberPreference
import com.programmersbox.uiviews.utils.update
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
            playingStartAction = PlayingStartAction.CurrentChapter
            playingMiddleAction = PlayingMiddleAction.Nothing
            useNewReader = true
            pagePadding = 4
            listOrPager = true
        }
    override val parseFrom: (input: InputStream) -> MangaSettings get() = MangaSettings::parseFrom
}

class MangaSettingsHandling(context: Context) {
    private val preferences by lazy { context.mangaSettings }
    private val all: Flow<MangaSettings> get() = preferences.data

    val playingStartAction = SettingInfo(
        flow = all.map { it.playingStartAction },
        updateValue = { setPlayingStartAction(it) }
    )

    @Composable
    fun rememberPlayingStartAction() = preferences.rememberPreference(
        key = { it.playingStartAction },
        update = { setPlayingStartAction(it) },
        defaultValue = PlayingStartAction.CurrentChapter
    )

    val playingMiddleAction = SettingInfo(
        flow = all.map { it.playingMiddleAction },
        updateValue = { setPlayingMiddleAction(it) }
    )

    @Composable
    fun rememberPlayingMiddleAction() = preferences.rememberPreference(
        key = { it.playingMiddleAction },
        update = { setPlayingMiddleAction(it) },
        defaultValue = PlayingMiddleAction.Nothing
    )

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

    val listOrPager = SettingInfo(
        flow = all.map { it.listOrPager },
        updateValue = { setListOrPager(it) }
    )

    @Composable
    fun rememberListOrPager() = preferences.rememberPreference(
        key = { it.listOrPager },
        update = { setListOrPager(it) },
        defaultValue = true
    )

    inner class SettingInfo<T>(
        val flow: Flow<T>,
        private val updateValue: suspend MangaSettings.Builder.(T) -> MangaSettings.Builder,
    ) {
        suspend fun updateSetting(value: T) = preferences.update { updateValue(value) }
    }
}