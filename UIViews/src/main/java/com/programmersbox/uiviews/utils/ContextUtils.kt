package com.programmersbox.uiviews.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
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
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.*
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.gsonutils.sharedPrefNotNullObjectDelegate
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.helpfulutils.*
import com.programmersbox.models.ChapterModel
import com.programmersbox.rxutils.toLatestFlowable
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asObservable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

var Context.currentService: String? by sharedPrefObjectDelegate(null)

var Context.shouldCheck: Boolean by sharedPrefNotNullObjectDelegate(true)

var Context.lastUpdateCheck: Long? by sharedPrefDelegate(null)
var Context.lastUpdateCheckEnd: Long? by sharedPrefDelegate(null)

val updateCheckPublish = BehaviorSubject.create<Long>()
val updateCheckPublishEnd = BehaviorSubject.create<Long>()

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

    val batteryLevelAlert = PublishSubject.create<Float>()
    val batteryInfoItem = PublishSubject.create<Battery>()

    enum class BatteryViewType(val icon: GoogleMaterial.Icon, val composeIcon: ImageVector) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full, Icons.Default.BatteryChargingFull),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std, Icons.Default.BatteryStd),
        FULL(GoogleMaterial.Icon.gmd_battery_full, Icons.Default.BatteryFull),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert, Icons.Default.BatteryAlert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown, Icons.Default.BatteryUnknown)
    }

    fun composeSetup(
        disposable: CompositeDisposable,
        normalBatteryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        subscribe: (Pair<androidx.compose.ui.graphics.Color, BatteryViewType>) -> Unit
    ) {
        Flowables.combineLatest(
            Observable.combineLatest(
                batteryLevelAlert,
                context.batteryPercent.asObservable()
            ) { b, d -> b <= d }
                .map { if (it) androidx.compose.ui.graphics.Color.Red else normalBatteryColor }
                .toLatestFlowable(),
            Observable.combineLatest(
                batteryInfoItem,
                context.batteryPercent.asObservable()
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
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(subscribe)
            .addTo(disposable)
    }

    fun setup(
        disposable: CompositeDisposable,
        normalBatteryColor: Int = Color.WHITE,
        size: Int,
        subscribe: (Pair<Int, IconicsDrawable>) -> Unit
    ) {
        Flowables.combineLatest(
            Observable.combineLatest(
                batteryLevelAlert,
                context.batteryPercent.asObservable()
            ) { b, d -> b <= d }
                .map { if (it) Color.RED else normalBatteryColor }
                .toLatestFlowable(),
            Observable.combineLatest(
                batteryInfoItem,
                context.batteryPercent.asObservable()
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
                .map { IconicsDrawable(context, it.icon).apply { sizePx = size } }
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(subscribe)
            .addTo(disposable)
    }

}

fun Context.showErrorToast() = runOnUIThread { Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show() }

fun Context.getSystemDateTimeFormat() = SimpleDateFormat(
    "${(DateFormat.getDateFormat(this) as SimpleDateFormat).toLocalizedPattern()} ${(DateFormat.getTimeFormat(this) as SimpleDateFormat).toLocalizedPattern()}",
    Locale.getDefault()
)


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