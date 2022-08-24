package com.programmersbox.uiviews.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.*
import android.graphics.*
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcel
import android.text.Spannable
import android.text.Spanned
import android.text.format.DateFormat
import android.text.method.TransformationMethod
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.*
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.helpfulutils.*
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.set
import kotlin.properties.Delegates

var Context.currentService: String? by sharedPrefObjectDelegate(null)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    "otakuworld",
    //produceMigrations = { listOf(SharedPreferencesMigration(it, "HelpfulUtils")) }
)

val BATTERY_PERCENT = intPreferencesKey("battery_percent")
val Context.batteryPercent get() = dataStore.data.map { it[BATTERY_PERCENT] ?: 20 }

val SHARE_CHAPTER = booleanPreferencesKey("share_chapter")
val Context.shareChapter get() = dataStore.data.map { it[SHARE_CHAPTER] ?: true }

val SHOULD_CHECK = booleanPreferencesKey("shouldCheckUpdate")
val Context.shouldCheckFlow get() = dataStore.data.map { it[SHOULD_CHECK] ?: true }

val THEME_SETTING = stringPreferencesKey("theme")
val Context.themeSetting get() = dataStore.data.map { it[THEME_SETTING] ?: "System" }

val SHOW_ALL = booleanPreferencesKey("show_all")
val Context.showAll get() = dataStore.data.map { it[SHOW_ALL] ?: true }

val HISTORY_SAVE = intPreferencesKey("history_save")
val Context.historySave get() = dataStore.data.map { it[HISTORY_SAVE] ?: 50 }

val UPDATE_CHECKING_START = longPreferencesKey("lastUpdateCheckStart")
val Context.updateCheckingStart get() = dataStore.data.map { it[UPDATE_CHECKING_START] ?: System.currentTimeMillis() }

val UPDATE_CHECKING_END = longPreferencesKey("lastUpdateCheckEnd")
val Context.updateCheckingEnd get() = dataStore.data.map { it[UPDATE_CHECKING_END] ?: System.currentTimeMillis() }

suspend fun <T> Context.updatePref(key: Preferences.Key<T>, value: T) = dataStore.edit { it[key] = value }

@JvmInline
value class NotificationLogo(val notificationId: Int)

fun Context.openInCustomChromeBrowser(url: Uri, build: CustomTabsIntent.Builder.() -> Unit = {}) = CustomTabsIntent.Builder()
    .setExitAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    .apply(build)
    .build().launchUrl(this, url)

fun Context.openInCustomChromeBrowser(url: String, build: CustomTabsIntent.Builder.() -> Unit = {}) = openInCustomChromeBrowser(Uri.parse(url), build)

private class CustomTabsURLSpan : URLSpan {
    private val context: Context
    private val builder: CustomTabsIntent.Builder.() -> Unit

    constructor(url: String?, context: Context, build: CustomTabsIntent.Builder.() -> Unit = {}) : super(url) {
        this.context = context
        this.builder = build
    }

    constructor(src: Parcel, context: Context, build: CustomTabsIntent.Builder.() -> Unit = {}) : super(src) {
        this.context = context
        this.builder = build
    }

    override fun onClick(widget: View) {
        context.openInCustomChromeBrowser(url, builder)
        // attempt to open with custom tabs, if that fails, call super.onClick
    }
}

class ChromeCustomTabTransformationMethod(private val context: Context, private val build: CustomTabsIntent.Builder.() -> Unit = {}) :
    TransformationMethod {
    override fun getTransformation(source: CharSequence, view: View?): CharSequence {
        if (view is TextView) {
            Linkify.addLinks(view, Linkify.WEB_URLS)
            if (view.text == null || view.text !is Spannable) return source
            val text: Spannable = view.text as Spannable
            val spans: Array<URLSpan> = text.getSpans(0, view.length(), URLSpan::class.java)
            for (i in spans.indices.reversed()) {
                val oldSpan = spans[i]
                val start: Int = text.getSpanStart(oldSpan)
                val end: Int = text.getSpanEnd(oldSpan)
                val url = oldSpan.url
                text.removeSpan(oldSpan)
                text.setSpan(CustomTabsURLSpan(url, context, build), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return text
        }
        return source
    }

    override fun onFocusChanged(
        view: View?,
        sourceText: CharSequence?,
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) = Unit
}

fun View.toolTipText(text: CharSequence) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tooltipText = text
}

fun View.toolTipText(@StringRes stringId: Int) = toolTipText(context.getString(stringId))

fun Bitmap.glowEffect(glowRadius: Int, glowColor: Int): Bitmap? {
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

class ChapterModelDeserializer(private val genericInfo: GenericInfo) : JsonDeserializer<ChapterModel> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ChapterModel? {
        return json.asJsonObject.let {
            genericInfo.toSource(it["source"].asString)?.let { it1 ->
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

class ApiServiceDeserializer(private val genericInfo: GenericInfo) : JsonDeserializer<ApiService> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ApiService? {
        return genericInfo.toSource(json.asString)
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

class BatteryInformation(val context: Context) {

    val batteryLevel by lazy { MutableStateFlow<Float>(0f) }
    val batteryInfo by lazy { MutableSharedFlow<Battery>() }

    enum class BatteryViewType(val icon: GoogleMaterial.Icon, val composeIcon: ImageVector) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full, Icons.Default.BatteryChargingFull),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std, Icons.Default.BatteryStd),
        FULL(GoogleMaterial.Icon.gmd_battery_full, Icons.Default.BatteryFull),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert, Icons.Default.BatteryAlert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown, Icons.Default.BatteryUnknown)
    }

    suspend fun composeSetupFlow(
        normalBatteryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        subscribe: suspend (Pair<androidx.compose.ui.graphics.Color, BatteryViewType>) -> Unit
    ) {
        combine(
            combine(
                batteryLevel,
                context.batteryPercent
            ) { b, d -> b <= d }
                .map { if (it) androidx.compose.ui.graphics.Color.Red else normalBatteryColor },
            combine(
                batteryInfo,
                context.batteryPercent
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
            .collect()
    }

    suspend fun setupFlow(
        normalBatteryColor: Int = Color.WHITE,
        size: Int,
        subscribe: (Pair<Int, IconicsDrawable>) -> Unit
    ) {
        combine(
            combine(
                batteryLevel,
                context.batteryPercent
            ) { b, d -> b <= d }
                .map { if (it) Color.RED else normalBatteryColor },
            combine(
                batteryInfo,
                context.batteryPercent
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

inline fun <reified V : ViewModel> factoryCreate(crossinline build: () -> V) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(V::class.java)) {
            return build() as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}

class DownloadUpdate(private val context: Context, private val packageName: String) : KoinComponent {

    val genericInfo: GenericInfo by inject()

    fun downloadUpdate(update: AppUpdate.AppUpdates): Boolean {
        val downloadManager = context.downloadManager
        val request = DownloadManager.Request(Uri.parse(update.downloadUrl(genericInfo.apkString)))
            .setMimeType("application/vnd.android.package-archive")
            .setTitle(context.getString(R.string.app_name))
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                update.downloadUrl(genericInfo.apkString).split("/").lastOrNull() ?: "update_apk"
            )
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            -1
        }
        if (id == -1L) return true
        context.registerReceiver(
            object : BroadcastReceiver() {
                @SuppressLint("Range")
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, id) ?: id

                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId)
                        val c = downloadManager.query(query)

                        if (c.moveToFirst()) {
                            val columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                                c.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI)
                                val uri = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)))
                                openApk(this@DownloadUpdate.context, uri)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        return true
    }

    private fun openApk(context: Context, uri: Uri) {
        uri.path?.let {
            val contentUri = FileProvider.getUriForFile(
                context,
                "$packageName.provider",
                File(it)
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                data = contentUri
            }
            context.startActivity(installIntent)
        }
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

val LocalActivity = staticCompositionLocalOf<FragmentActivity> { error("Context is not an Activity.") }

fun Context.findActivity(): FragmentActivity {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    error("Context is not an Activity.")
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
    vararg key: Any
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