package com.programmersbox.uiviews.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.protobuf.GeneratedMessageLite
import com.google.protobuf.InvalidProtocolBufferException
import com.programmersbox.uiviews.GridChoice
import com.programmersbox.uiviews.MiddleNavigationAction
import com.programmersbox.uiviews.NotificationSortBy
import com.programmersbox.uiviews.Settings
import com.programmersbox.uiviews.SystemThemeMode
import com.programmersbox.uiviews.ThemeColor
import com.programmersbox.uiviews.middleMultipleActions
import com.programmersbox.uiviews.settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

suspend fun <DS : DataStore<MessageType>, MessageType : GeneratedMessageLite<MessageType, BuilderType>, BuilderType : GeneratedMessageLite.Builder<MessageType, BuilderType>> DS.update(
    statsBuilder: suspend BuilderType.() -> BuilderType,
) = updateData { statsBuilder(it.toBuilder()).build() }

interface GenericSerializer<MessageType, BuilderType> : Serializer<MessageType>
        where MessageType : GeneratedMessageLite<MessageType, BuilderType>,
              BuilderType : GeneratedMessageLite.Builder<MessageType, BuilderType> {

    /**
     * Call MessageType::parseFrom here!
     */
    val parseFrom: (input: InputStream) -> MessageType

    override suspend fun readFrom(input: InputStream): MessageType =
        withContext(Dispatchers.IO) {
            try {
                parseFrom(input)
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            }
        }

    override suspend fun writeTo(t: MessageType, output: OutputStream) =
        withContext(Dispatchers.IO) { t.writeTo(output) }
}

val Context.settings: DataStore<Settings> by dataStore(
    fileName = "Settings",
    serializer = SettingsSerializer
)

object SettingsSerializer : GenericSerializer<Settings, Settings.Builder> {
    override val defaultValue: Settings
        get() = settings {
            batteryPercent = 20
            historySave = 50
            shareChapter = true
            showAll = true
            shouldCheckUpdate = true
            themeSetting = SystemThemeMode.FollowSystem
            showListDetail = true
            showDownload = true
            amoledMode = false
            usePalette = true
            showBlur = true
            showExpressiveness = true
            multipleActions = middleMultipleActions {
                startAction = MiddleNavigationAction.All
                endAction = MiddleNavigationAction.Notifications
            }
        }
    override val parseFrom: (input: InputStream) -> Settings get() = Settings::parseFrom
}

class SettingsHandling(context: Context) {
    private val preferences by lazy { context.settings }
    private val all: Flow<Settings> get() = preferences.data

    @Composable
    fun rememberSystemThemeMode() = preferences.rememberPreference(
        key = { it.themeSetting },
        update = { setThemeSetting(it) },
        defaultValue = SystemThemeMode.FollowSystem
    )

    val batteryPercent = ProtoStoreHandler(
        preferences = preferences,
        key = { it.batteryPercent },
        update = { setBatteryPercent(it) },
        defaultValue = 20
    )

    @Composable
    fun rememberShareChapter() = preferences.rememberPreference(
        key = { it.shareChapter },
        update = { setShareChapter(it) },
        defaultValue = true
    )

    @Composable
    fun rememberShowAll() = preferences.rememberPreference(
        key = { it.showAll },
        update = { setShowAll(it) },
        defaultValue = false
    )

    val notificationSortBy = ProtoStoreHandler(
        preferences = preferences,
        key = { it.notificationSortBy },
        update = { setNotificationSortBy(it) },
        defaultValue = NotificationSortBy.Date
    )

    @Composable
    fun rememberShowListDetail() = preferences.rememberPreference(
        key = { it.showListDetail },
        update = { setShowListDetail(it) },
        defaultValue = true
    )

    val customUrls = all.map { it.customUrlsList }

    suspend fun addCustomUrl(url: String) = preferences.update { addCustomUrls(url) }
    suspend fun removeCustomUrl(url: String) = preferences.update {
        val l = customUrlsList.toMutableList()
        l.remove(url)
        clearCustomUrls()
        addAllCustomUrls(l)
    }

    @Composable
    fun rememberShowDownload() = preferences.rememberPreference(
        key = { it.showDownload },
        update = { setShowDownload(it) },
        defaultValue = false
    )

    @Composable
    fun rememberIsAmoledMode() = preferences.rememberPreference(
        key = { it.amoledMode },
        update = { setAmoledMode(it) },
        defaultValue = false
    )

    @Composable
    fun rememberUsePalette() = preferences.rememberPreference(
        key = { it.usePalette },
        update = { setUsePalette(it) },
        defaultValue = true
    )

    @Composable
    fun rememberShowBlur() = preferences.rememberPreference(
        key = { it.showBlur },
        update = { setShowBlur(it) },
        defaultValue = true
    )

    @Composable
    fun rememberGridChoice() = preferences.rememberPreference(
        key = { it.gridChoice },
        update = { setGridChoice(it) },
        defaultValue = GridChoice.FullAdaptive
    )

    @Composable
    fun rememberThemeColor() = preferences.rememberPreference(
        key = { it.themeColor },
        update = { setThemeColor(it) },
        defaultValue = ThemeColor.Dynamic
    )

    @Composable
    fun rememberShowExpressiveness() = preferences.rememberPreference(
        key = { it.showExpressiveness },
        update = { setShowExpressiveness(it) },
        defaultValue = true
    )

    @Composable
    fun rememberMiddleNavigationAction() = preferences.rememberPreference(
        key = { it.middleNavigationAction },
        update = { setMiddleNavigationAction(it) },
        defaultValue = MiddleNavigationAction.All,
    )

    @Composable
    fun rememberMiddleMultipleActions() = preferences.rememberPreference(
        key = { it.multipleActions },
        update = { setMultipleActions(it) },
        defaultValue = middleMultipleActions {
            startAction = MiddleNavigationAction.All
            endAction = MiddleNavigationAction.Notifications
        },
    )
}

@Composable
fun <T, DS, MessageType, BuilderType> DS.rememberPreference(
    key: (MessageType) -> T,
    update: BuilderType.(T) -> BuilderType,
    defaultValue: T,
): MutableState<T> where DS : DataStore<MessageType>,
                         MessageType : GeneratedMessageLite<MessageType, BuilderType>,
                         BuilderType : GeneratedMessageLite.Builder<MessageType, BuilderType> {
    val coroutineScope = rememberCoroutineScope()
    val state by remember { data.map(key) }.collectAsStateWithLifecycle(initialValue = defaultValue)

    return remember(state) {
        object : MutableState<T> {
            override var value: T
                get() = state
                set(value) {
                    coroutineScope.launch {
                        update { update(value) }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

class ProtoStoreHandler<T, DS, MessageType, BuilderType>(
    private val preferences: DS,
    private val key: (MessageType) -> T,
    private val update: BuilderType.(T) -> BuilderType,
    private val defaultValue: T,
) where DS : DataStore<MessageType>,
        MessageType : GeneratedMessageLite<MessageType, BuilderType>,
        BuilderType : GeneratedMessageLite.Builder<MessageType, BuilderType> {

    fun asFlow() = preferences.data.map { key(it) }

    suspend fun get() = asFlow().firstOrNull() ?: defaultValue

    suspend fun set(value: T) {
        preferences.update { update(value) }
    }

    @Composable
    fun rememberPreference(): MutableState<T> {
        val coroutineScope = rememberCoroutineScope()
        val state by remember { preferences.data.map(key) }.collectAsStateWithLifecycle(initialValue = defaultValue)

        return remember(state) {
            object : MutableState<T> {
                override var value: T
                    get() = state
                    set(value) {
                        coroutineScope.launch {
                            preferences.update { update(value) }
                        }
                    }

                override fun component1() = value
                override fun component2(): (T) -> Unit = { value = it }
            }
        }
    }
}