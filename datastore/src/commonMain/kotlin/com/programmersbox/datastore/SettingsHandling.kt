package com.programmersbox.datastore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.okio.OkioSerializer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.BufferedSource

interface GenericSerializer<MessageType> : OkioSerializer<MessageType> {
    /*where MessageType : GeneratedMessageLite<MessageType, BuilderType>,
          BuilderType : GeneratedMessageLite.Builder<MessageType, BuilderType> {*/

    /**
     * Call MessageType::parseFrom here!
     */
    val parseFrom: (input: BufferedSource) -> MessageType

    override suspend fun readFrom(source: BufferedSource): MessageType {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext parseFrom(source)
            } catch (exception: IOException) {
                throw Exception(exception.message ?: "Serialization Exception")
            }
        }
    }

    override suspend fun writeTo(t: MessageType, sink: BufferedSink) {
        withContext(Dispatchers.IO) { sink.write(encode(t)) }
    }

    fun encode(t: MessageType): ByteArray

    /*override suspend fun readFrom(input: InputStream): MessageType =
        withContext(Dispatchers.IO) {
            try {
                parseFrom(input)
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            }
        }*/

    /*override suspend fun writeTo(t: MessageType, output: OutputStream) =
        withContext(Dispatchers.IO) { t.writeTo(output) }*/
}

/*val settings: DataStore<Settings> by dataStore(
    fileName = "Settings",
    serializer = SettingsSerializer
)*/

object SettingsSerializer : GenericSerializer<Settings> {
    override val defaultValue: Settings
        get() = Settings(
            batteryPercent = 20,
            historySave = 50,
            shareChapter = true,
            showAll = true,
            shouldCheckUpdate = true,
            themeSetting = SystemThemeMode.FollowSystem,
            showListDetail = true,
            showDownload = true,
            amoledMode = false,
            usePalette = true,
            showBlur = true,//PerformanceClass.canBlur(),
            showExpressiveness = true,
            notifyOnReboot = true,
            multipleActions = MiddleMultipleActions(
                startAction = MiddleNavigationAction.All,
                endAction = MiddleNavigationAction.Notifications,
            ),
        )

    override val parseFrom: (input: BufferedSource) -> Settings get() = Settings.ADAPTER::decode

    override fun encode(t: Settings): ByteArray = t.encode()
}

class NewSettingsHandling(
    val preferences: DataStore<Settings>,
    //private val performanceClass: PerformanceClass,
) {
    private val all: Flow<Settings> get() = preferences.data

    @Composable
    fun rememberSystemThemeMode() = preferences.rememberPreference(
        key = { it.themeSetting },
        update = { copy(themeSetting = it) },
        defaultValue = SystemThemeMode.FollowSystem
    )

    val batteryPercent = ProtoStoreHandler(
        preferences = preferences,
        key = { it.batteryPercent },
        update = { copy(batteryPercent = it) },
        defaultValue = 20
    )

    val notifyOnReboot = ProtoStoreHandler(
        preferences = preferences,
        key = { it.notifyOnReboot },
        update = { copy(notifyOnReboot = it) },
        defaultValue = true
    )

    @Composable
    fun rememberShareChapter() = preferences.rememberPreference(
        key = { it.shareChapter },
        update = { copy(shareChapter = it) },
        defaultValue = true
    )

    @Composable
    fun rememberShowAll() = preferences.rememberPreference(
        key = { it.showAll },
        update = { copy(showAll = it) },
        defaultValue = false
    )

    val notificationSortBy = ProtoStoreHandler(
        preferences = preferences,
        key = { it.notificationSortBy },
        update = { copy(notificationSortBy = it) },
        defaultValue = NotificationSortBy.Date
    )

    @Composable
    fun rememberShowListDetail() = preferences.rememberPreference(
        key = { it.showListDetail },
        update = { copy(showListDetail = it) },
        defaultValue = true
    )

    val customUrls = all.map { it.customUrls }

    suspend fun addCustomUrl(url: String) = preferences.updateData {
        it.copy(customUrls = it.customUrls + url)
    }

    suspend fun removeCustomUrl(url: String) = preferences.updateData {
        val l = it.customUrls.toMutableList()
        l.remove(url)
        it.copy(customUrls = l)
    }

    @Composable
    fun rememberShowDownload() = preferences.rememberPreference(
        key = { it.showDownload },
        update = { copy(showDownload = it) },
        defaultValue = false
    )

    @Composable
    fun rememberIsAmoledMode() = preferences.rememberPreference(
        key = { it.amoledMode },
        update = { copy(amoledMode = it) },
        defaultValue = true
    )

    @Composable
    fun rememberUsePalette() = preferences.rememberPreference(
        key = { it.usePalette },
        update = { copy(usePalette = it) },
        defaultValue = true
    )

    @Composable
    fun rememberShowBlur() = preferences.rememberPreference(
        key = { it.showBlur },
        update = { copy(showBlur = it) },
        defaultValue = true//performanceClass.canBlur
    )

    @Composable
    fun rememberGridChoice() = preferences.rememberPreference(
        key = { it.gridChoice },
        update = { copy(gridChoice = it) },
        defaultValue = GridChoice.FullAdaptive
    )

    @Composable
    fun rememberThemeColor() = preferences.rememberPreference(
        key = { it.themeColor },
        update = { copy(themeColor = it) },
        defaultValue = ThemeColor.Dynamic
    )

    @Composable
    fun rememberShowExpressiveness() = preferences.rememberPreference(
        key = { it.showExpressiveness },
        update = { copy(showExpressiveness = it) },
        defaultValue = true
    )

    @Composable
    fun rememberMiddleNavigationAction() = preferences.rememberPreference(
        key = { it.middleNavigationAction },
        update = { copy(middleNavigationAction = it) },
        defaultValue = MiddleNavigationAction.All,
    )

    @Composable
    fun rememberMiddleMultipleActions() = preferences.rememberPreference(
        key = { it.multipleActions },
        update = { copy(multipleActions = it) },
        defaultValue = MiddleMultipleActions(
            startAction = MiddleNavigationAction.All,
            endAction = MiddleNavigationAction.Notifications
        )
    )
}

@Composable
fun <T, DS, MessageType> DS.rememberPreference(
    key: (MessageType) -> T,
    update: MessageType.(T) -> MessageType,
    defaultValue: T,
): MutableState<T> where DS : DataStore<MessageType> {
    val coroutineScope = rememberCoroutineScope()
    val state by remember { data.map(key) }.collectAsStateWithLifecycle(initialValue = defaultValue)

    return remember(state) {
        object : MutableState<T> {
            override var value: T
                get() = state
                set(value) {
                    coroutineScope.launch {
                        updateData { it.update(value) }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

class ProtoStoreHandler<T, DS, MessageType>(
    private val preferences: DS,
    private val key: (MessageType) -> T,
    private val update: MessageType.(T) -> MessageType,
    private val defaultValue: T,
) where DS : DataStore<MessageType> {

    fun asFlow() = preferences.data.map { key(it) }

    suspend fun get() = asFlow().firstOrNull() ?: defaultValue

    suspend fun set(value: T) {
        preferences.updateData { it.update(value) }
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
                            this@ProtoStoreHandler.set(value)
                        }
                    }

                override fun component1() = value
                override fun component2(): (T) -> Unit = { value = it }
            }
        }
    }
}