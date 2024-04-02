package com.programmersbox.uiviews.utils

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.helpfulutils.Battery
import com.programmersbox.helpfulutils.BatteryHealth
import com.programmersbox.helpfulutils.connectivityManager
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.collections.set
import kotlin.properties.Delegates
import kotlin.properties.PropertyDelegateProvider

var Context.currentService: String? by sharedPrefObjectDelegate(null)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    "otakuworld",
    //produceMigrations = { listOf(SharedPreferencesMigration(it, "HelpfulUtils")) }
)

val SHOULD_CHECK = booleanPreferencesKey("shouldCheckUpdate")
val Context.shouldCheckFlow get() = dataStore.data.map { it[SHOULD_CHECK] ?: true }

val HISTORY_SAVE = intPreferencesKey("history_save")
val Context.historySave get() = dataStore.data.map { it[HISTORY_SAVE] ?: 50 }

val UPDATE_CHECKING_START = longPreferencesKey("lastUpdateCheckStart")
val Context.updateCheckingStart get() = dataStore.data.map { it[UPDATE_CHECKING_START] ?: System.currentTimeMillis() }

val UPDATE_CHECKING_END = longPreferencesKey("lastUpdateCheckEnd")
val Context.updateCheckingEnd get() = dataStore.data.map { it[UPDATE_CHECKING_END] ?: System.currentTimeMillis() }

suspend fun <T> Context.updatePref(key: Preferences.Key<T>, value: T) = dataStore.edit { it[key] = value }

@Composable
fun rememberHistorySave() = rememberPreference(
    key = HISTORY_SAVE,
    defaultValue = 50
)

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val state by remember {
        context.dataStore.data
            .map { it[key] ?: defaultValue }
    }.collectAsStateWithLifecycle(initialValue = defaultValue)

    return remember(state) {
        object : MutableState<T> {
            override var value: T
                get() = state
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit { it[key] = value }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@JvmInline
value class NotificationLogo(val notificationId: Int)

fun Context.openInCustomChromeBrowser(url: Uri, build: CustomTabsIntent.Builder.() -> Unit = {}) = CustomTabsIntent.Builder()
    .setExitAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    .apply(build)
    .build().launchUrl(this, url)

fun Context.openInCustomChromeBrowser(url: String, build: CustomTabsIntent.Builder.() -> Unit = {}) = openInCustomChromeBrowser(Uri.parse(url), build)

fun View.toolTipText(text: CharSequence) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tooltipText = text
}

fun View.toolTipText(@StringRes stringId: Int) = toolTipText(context.getString(stringId))

fun Bitmap.glowEffect(glowRadius: Int, glowColor: Int): Bitmap {
    val margin = 24
    val halfMargin = margin / 2f
    val alpha = extractAlpha()
    val out = Bitmap.createBitmap(width + margin, height + margin, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint()
    paint.color = glowColor

    // Outer glow, For Inner glow set Blur.INNER
    paint.maskFilter = BlurMaskFilter(glowRadius.toFloat(), BlurMaskFilter.Blur.OUTER)
    canvas.drawBitmap(alpha, halfMargin, halfMargin, paint)
    canvas.drawBitmap(this, halfMargin, halfMargin, null)
    alpha.recycle()
    return out
}

class ChapterModelSerializer : JsonSerializer<ChapterModel> {
    override fun serialize(src: ChapterModel, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val json = JsonObject()
        json.addProperty("name", src.name)
        json.addProperty("uploaded", src.uploaded)
        json.addProperty("source", src.source.serviceName)
        json.addProperty("sourceUrl", src.sourceUrl)
        json.addProperty("url", src.url)
        return json
    }
}

class ChapterModelDeserializer : JsonDeserializer<ChapterModel>, KoinComponent {
    private val sourceRepository: SourceRepository by inject<SourceRepository>()
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChapterModel? {
        return json.asJsonObject.let {
            sourceRepository.toSourceByApiServiceName(it["source"].asString)
                ?.apiService
                ?.let { it1 ->
                    ChapterModel(
                        name = it["name"].asString,
                        uploaded = it["uploaded"].asString,
                        source = it1,
                        sourceUrl = it["sourceUrl"].asString,
                        url = it["url"].asString
                    )
                }
        }
    }
}

class ApiServiceSerializer : JsonSerializer<ApiService> {
    override fun serialize(src: ApiService, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return context.serialize(src.serviceName)
    }
}

class ApiServiceDeserializer(private val genericInfo: GenericInfo) : JsonDeserializer<ApiService>, KoinComponent {
    private val sourceRepository: SourceRepository by inject()
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ApiService? {
        return sourceRepository.toSourceByApiServiceName(json.asString)?.apiService ?: genericInfo.toSource(json.asString)
    }
}

fun <T : View> BottomSheetBehavior<T>.open() {
    state = BottomSheetBehavior.STATE_EXPANDED
}

fun <T : View> BottomSheetBehavior<T>.close() {
    state = BottomSheetBehavior.STATE_COLLAPSED
}

fun <T : View> BottomSheetBehavior<T>.halfOpen() {
    state = BottomSheetBehavior.STATE_HALF_EXPANDED
}

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = super.onCreateDialog(savedInstanceState)
        .apply {
            setOnShowListener {
                val sheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                val bottomSheet = BottomSheetBehavior.from(sheet)
                bottomSheet.skipCollapsed = true
                bottomSheet.isDraggable = false
                bottomSheet.open()
            }
        }
}

class BatteryInformation(val context: Context) : KoinComponent {

    val batteryLevel by lazy { MutableStateFlow<Float>(0f) }
    val batteryInfo by lazy { MutableSharedFlow<Battery>() }
    val settingsHandling: SettingsHandling by inject()

    enum class BatteryViewType(val icon: GoogleMaterial.Icon, val composeIcon: ImageVector) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full, Icons.Default.BatteryChargingFull),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std, Icons.Default.BatteryStd),
        FULL(GoogleMaterial.Icon.gmd_battery_full, Icons.Default.BatteryFull),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert, Icons.Default.BatteryAlert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown, Icons.Default.BatteryUnknown)
    }

    suspend fun composeSetupFlow(
        normalBatteryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        subscribe: suspend (Pair<androidx.compose.ui.graphics.Color, BatteryViewType>) -> Unit,
    ) = combine(
        combine(
            batteryLevel,
            settingsHandling.batteryPercentage
        ) { b, d -> b <= d }
            .map { if (it) androidx.compose.ui.graphics.Color.Red else normalBatteryColor },
        combine(
            batteryInfo,
            settingsHandling.batteryPercentage
        ) { b, d -> b to d }
            .map {
                when {
                    it.first.isCharging -> BatteryViewType.CHARGING_FULL
                    it.first.percent <= it.second -> BatteryViewType.ALERT
                    it.first.percent >= 95 -> BatteryViewType.FULL
                    it.first.health == BatteryHealth.UNKNOWN -> BatteryViewType.UNKNOWN
                    else -> BatteryViewType.DEFAULT
                }
            }
            .distinctUntilChanged { t1, t2 -> t1 != t2 },
    ) { l, b -> l to b }
        .onEach(subscribe)

    suspend fun setupFlow(
        normalBatteryColor: Int = Color.WHITE,
        size: Int,
        subscribe: (Pair<Int, IconicsDrawable>) -> Unit,
    ) {
        combine(
            combine(
                batteryLevel,
                settingsHandling.batteryPercentage
            ) { b, d -> b <= d }
                .map { if (it) Color.RED else normalBatteryColor },
            combine(
                batteryInfo,
                settingsHandling.batteryPercentage
            ) { b, d -> b to d }
                .map {
                    when {
                        it.first.isCharging -> BatteryViewType.CHARGING_FULL
                        it.first.percent <= it.second -> BatteryViewType.ALERT
                        it.first.percent >= 95 -> BatteryViewType.FULL
                        it.first.health == BatteryHealth.UNKNOWN -> BatteryViewType.UNKNOWN
                        else -> BatteryViewType.DEFAULT
                    }
                }
                .distinctUntilChanged { t1, t2 -> t1 != t2 }
                .map { IconicsDrawable(context, it.icon).apply { sizePx = size } },
        ) { l, b -> l to b }
            .onEach(subscribe)
            .collect()
    }

}

fun Context.showErrorToast() = runOnUIThread { Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show() }

fun Context.getSystemDateTimeFormat() = SimpleDateFormat(
    "${(DateFormat.getDateFormat(this) as SimpleDateFormat).toLocalizedPattern()} ${(DateFormat.getTimeFormat(this) as SimpleDateFormat).toLocalizedPattern()}",
    Locale.getDefault()
)

val LocalSystemDateTimeFormat = staticCompositionLocalOf<SimpleDateFormat> { error("Nothing here!") }

inline fun <reified V : ViewModel> factoryCreate(crossinline build: () -> V) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(V::class.java)) {
            return build() as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}

object Cached {

    private val map = mutableMapOf<String, InfoModel>()

    val cache = ExpirableLRUCache<String, InfoModel>(
        minimalSize = 10,
        flushInterval = TimeUnit.MINUTES.toMillis(5)
    ) {
        size = map.size
        set { key, value -> map[key] = value }
        get { map[it] }
        remove { map.remove(it) }
        clear { map.clear() }
    }

}

@DslMarker
annotation class GenericCacheMarker

/**
 * A Generic K,V [GenericCache] defines the basic operations to a cache.
 */
interface GenericCache<K, V> {
    /**
     * The number of the items that are currently cached.
     */
    val size: Int

    /**
     * Cache a [value] with a given [key]
     */
    operator fun set(key: K, value: V)

    /**
     * Get the cached value of a given [key], or null if it's not cached or evicted.
     */
    operator fun get(key: K): V?

    /**
     * Remove the value of the [key] from the cache, and return the removed value, or null if it's not cached at all.
     */
    fun remove(key: K): V?

    /**
     * Remove all the items in the cache.
     */
    fun clear()

    class GenericCacheBuilder<K, V> {
        @GenericCacheMarker
        var size: Int by Delegates.notNull()
        private var set: (key: K, value: V) -> Unit by Delegates.notNull()
        private var get: (key: K) -> V? by Delegates.notNull()
        private var remove: (key: K) -> V? by Delegates.notNull()
        private var clear: () -> Unit by Delegates.notNull()

        @GenericCacheMarker
        fun set(block: (key: K, value: V) -> Unit) {
            set = block
        }

        @GenericCacheMarker
        fun get(block: (key: K) -> V?) {
            get = block
        }

        @GenericCacheMarker
        fun remove(block: (key: K) -> V?) {
            remove = block
        }

        @GenericCacheMarker
        fun clear(block: () -> Unit) {
            clear = block
        }

        internal fun build() = object : GenericCache<K, V> {
            override val size: Int get() = this@GenericCacheBuilder.size
            override fun set(key: K, value: V) = this@GenericCacheBuilder.set(key, value)
            override fun get(key: K): V? = this@GenericCacheBuilder.get(key)
            override fun remove(key: K): V? = this@GenericCacheBuilder.remove(key)
            override fun clear() = this@GenericCacheBuilder.clear()
        }
    }
}

/**
 * [ExpirableLRUCache] flushes items that are **Least Recently Used** and keeps [minimalSize] items at most
 * along with flushing the items whose life time is longer than [flushInterval].
 */
class ExpirableLRUCache<K, V>(
    private val minimalSize: Int = DEFAULT_SIZE,
    private val flushInterval: Long = TimeUnit.MINUTES.toMillis(1),
    private val delegate: GenericCache<K, V>,
) : GenericCache<K, V> by delegate {

    constructor(minimalSize: Int, flushInterval: Long, delegate: GenericCache.GenericCacheBuilder<K, V>.() -> Unit) : this(
        minimalSize,
        flushInterval,
        GenericCache.GenericCacheBuilder<K, V>().apply(delegate).build()
    )

    private val keyMap = object : LinkedHashMap<K, Boolean>(minimalSize, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Boolean>): Boolean {
            val tooManyCachedItems = size > minimalSize
            if (tooManyCachedItems) eldestKeyToRemove = eldest.key
            return tooManyCachedItems
        }
    }

    private var lastFlushTime = System.nanoTime()

    private var eldestKeyToRemove: K? = null

    override val size: Int
        get() {
            recycle()
            return delegate.size
        }

    override fun set(key: K, value: V) {
        recycle()
        delegate[key] = value
        cycleKeyMap(key)
    }

    override fun remove(key: K): V? {
        recycle()
        return delegate.remove(key)
    }

    override fun get(key: K): V? {
        recycle()
        keyMap[key]
        return delegate[key]
    }

    override fun clear() {
        keyMap.clear()
        delegate.clear()
    }

    private fun cycleKeyMap(key: K) {
        keyMap[key] = PRESENT
        eldestKeyToRemove?.let { delegate.remove(it) }
        eldestKeyToRemove = null
    }

    private fun recycle() {
        val shouldRecycle = System.nanoTime() - lastFlushTime >= TimeUnit.MILLISECONDS.toNanos(flushInterval)
        if (shouldRecycle) {
            delegate.clear()
            lastFlushTime = System.nanoTime()
        }
    }

    companion object {
        private const val DEFAULT_SIZE = 100
        private const val PRESENT = true
    }
}

val LocalSettingsHandling = staticCompositionLocalOf<SettingsHandling> { error("Not Set") }

val LocalActivity = staticCompositionLocalOf<FragmentActivity> { error("Context is not an Activity.") }

tailrec fun Context.findActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> this.baseContext.findActivity()
    else -> error("Could not find activity in Context chain.")
}

@Composable
inline fun <reified VM : ViewModel> viewModelInRoute(
    route: String,
    noinline initializer: CreationExtras.() -> VM,
): VM {
    val entry = rememberBackStackEntry(route)
    return viewModel(viewModelStoreOwner = entry, initializer = initializer)
}

@Composable
fun rememberBackStackEntry(
    route: String,
): NavBackStackEntry {
    val controller = LocalNavController.current
    return remember(controller.currentBackStackEntry) { controller.getBackStackEntry(route) }
}

val LocalNavController = staticCompositionLocalOf<NavHostController> { error("No NavController Found!") }
val LocalGenericInfo = staticCompositionLocalOf<GenericInfo> { error("No Info") }

enum class Status { Available, Losing, Lost, Unavailable }

@RequiresApi(Build.VERSION_CODES.N)
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
@Composable
fun connectivityStatus(
    initialValue: Status = Status.Unavailable,
    context: Context = LocalContext.current,
    vararg key: Any,
): State<Status> = produceState(initialValue = initialValue, keys = key) {
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            value = Status.Available
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            value = Status.Losing
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            value = Status.Lost
        }

        override fun onUnavailable() {
            super.onUnavailable()
            value = Status.Unavailable
        }
    }

    val manager = context.connectivityManager
    manager.registerDefaultNetworkCallback(callback)

    awaitDispose { manager.unregisterNetworkCallback(callback) }
}

val Context.appVersion: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0L)
        ).versionName
    } else {
        packageManager.getPackageInfo(packageName, 0)?.versionName
    }.orEmpty()

@Composable
fun appVersion(): String {
    return if (LocalInspectionMode.current) {
        "1.0.0"
    } else {
        val context = LocalContext.current
        remember(context) { context.appVersion }
    }
}

internal inline fun <T> SavedStateHandle.state(
    key: String? = null,
    crossinline initialValue: () -> T,
) = PropertyDelegateProvider<Any, MutableState<T>> { thisRef, property ->
    val internalKey = key ?: "${thisRef.javaClass.name}#${property.name}"
    getMutableState(internalKey, initialValue())
}

internal fun <T> SavedStateHandle.getMutableState(
    key: String,
    initialValue: T,
): MutableState<T> {
    val stateFlow = getStateFlow(key, initialValue)
    // Sync initial value with `stateFlow`.
    val base = mutableStateOf(stateFlow.value)

    return object : MutableState<T> by base {

        override var value: T
            get() {
                // Sync `base` value with `stateFlow`.
                if (base.value != stateFlow.value) {
                    base.value = stateFlow.value
                }
                return base.value
            }
            set(value) {
                // Sync `base` value with `stateFlow`.
                if (stateFlow.value != value) {
                    set(key, value)
                    base.value = get<T>(key) ?: value
                }
            }
    }
}