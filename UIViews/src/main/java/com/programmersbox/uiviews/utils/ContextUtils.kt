package com.programmersbox.uiviews.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BatteryUnknown
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.helpfulutils.Battery
import com.programmersbox.helpfulutils.BatteryHealth
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Locale

@JvmInline
value class NotificationLogo(val notificationId: Int)

fun Context.openInCustomChromeBrowser(url: Uri, build: CustomTabsIntent.Builder.() -> Unit = {}) = CustomTabsIntent.Builder()
    .setExitAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    .apply(build)
    .build().launchUrl(this, url)

fun Context.openInCustomChromeBrowser(url: String, build: CustomTabsIntent.Builder.() -> Unit = {}) = openInCustomChromeBrowser(Uri.parse(url), build)

fun View.toolTipText(text: CharSequence) {
    tooltipText = text
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

/*data object ChapterModelKSerializer : KSerializer<ChapterModel>, KoinComponent {

    private val sourceRepository: SourceRepository by inject<SourceRepository>()
    private val genericInfo: GenericInfo by inject<GenericInfo>()

    override val descriptor = buildClassSerialDescriptor("chapter_model") {
        element<String>("name")
        element<String>("uploaded")
        element<String>("source")
        element<String>("sourceUrl")
        element<String>("url")
        element<Map<String, Any>>("extras")
        element<Map<String, Any>>("otherExtras")
    }

    override fun deserialize(decoder: Decoder): ChapterModel {
        return ChapterModel(
            name = decoder.decodeString(),
            uploaded = decoder.decodeString(),
            source = decoder.decodeString().let {
                sourceRepository
                    .toSourceByApiServiceName(it)
                    ?.apiService
                    ?: genericInfo.toSource(it)!!
            },
            sourceUrl = decoder.decodeString(),
            url = decoder.decodeString()
        ).apply {
            extras.putAll(
                decoder.decodeSerializableValue(
                    MapSerializer(String.serializer(), String.serializer())
                )
            )
            otherExtras.putAll(
                decoder.decodeSerializableValue(
                    MapSerializer(String.serializer(), String.serializer())
                )
            )
        }
    }

    override fun serialize(encoder: Encoder, value: ChapterModel) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.name)
            encodeStringElement(descriptor, 1, value.uploaded)
            encodeStringElement(descriptor, 2, value.source.serviceName)
            encodeStringElement(descriptor, 3, value.sourceUrl)
            encodeStringElement(descriptor, 4, value.url)
            encodeSerializableElement(
                descriptor,
                5,
                MapSerializer(String.serializer(), String.serializer()),
                value.extras.mapValues { it.value.toString() }
            )
            encodeSerializableElement(
                descriptor,
                6,
                MapSerializer(String.serializer(), String.serializer()),
                value.otherExtras.mapValues { it.value.toString() }
            )
        }
    }
}*/

//TODO: Kotlinx Serialization this!
class ChapterModelSerializer : JsonSerializer<KmpChapterModel> {
    override fun serialize(src: KmpChapterModel, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val json = JsonObject()
        json.addProperty("name", src.name)
        json.addProperty("uploaded", src.uploaded)
        json.addProperty("source", src.source.serviceName)
        json.addProperty("sourceUrl", src.sourceUrl)
        json.addProperty("url", src.url)
        return json
    }
}

class ChapterModelDeserializer : JsonDeserializer<KmpChapterModel>, KoinComponent {
    private val sourceRepository: SourceRepository by inject<SourceRepository>()
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): KmpChapterModel? {
        return json.asJsonObject.let {
            sourceRepository.toSourceByApiServiceName(it["source"].asString)
                ?.apiService
                ?.let { it1 ->
                    KmpChapterModel(
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

class ApiServiceSerializer : JsonSerializer<KmpApiService> {
    override fun serialize(src: KmpApiService, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return context.serialize(src.serviceName)
    }
}

class ApiServiceDeserializer(private val genericInfo: GenericInfo) : JsonDeserializer<KmpApiService>, KoinComponent {
    private val sourceRepository: SourceRepository by inject()
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): KmpApiService? {
        return sourceRepository.toSourceByApiServiceName(json.asString)?.apiService
    }
}

//TODO: Put in own file
class BatteryInformation(val context: Context) : KoinComponent {

    val batteryLevel by lazy { MutableStateFlow<Float>(0f) }
    val batteryInfo by lazy { MutableSharedFlow<Battery>() }
    val settingsHandling: NewSettingsHandling by inject()

    private val batteryPercent by lazy { settingsHandling.batteryPercent }

    enum class BatteryViewType(val icon: GoogleMaterial.Icon, val composeIcon: ImageVector) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full, Icons.Default.BatteryChargingFull),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std, Icons.Default.BatteryStd),
        FULL(GoogleMaterial.Icon.gmd_battery_full, Icons.Default.BatteryFull),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert, Icons.Default.BatteryAlert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown, Icons.AutoMirrored.Filled.BatteryUnknown)
    }

    suspend fun composeSetupFlow(
        normalBatteryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        subscribe: suspend (Pair<androidx.compose.ui.graphics.Color, BatteryViewType>) -> Unit,
    ) = combine(
        combine(
            batteryLevel,
            batteryPercent.asFlow()
        ) { b, d -> b <= d }
            .map { if (it) androidx.compose.ui.graphics.Color.Red else normalBatteryColor },
        combine(
            batteryInfo,
            batteryPercent.asFlow()
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
                batteryPercent.asFlow()
            ) { b, d -> b <= d }
                .map { if (it) Color.RED else normalBatteryColor },
            combine(
                batteryInfo,
                batteryPercent.asFlow()
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

val LocalGenericInfo = staticCompositionLocalOf<GenericInfo> { error("No Info") }

val Context.appVersion: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0L)
        ).versionName
    } else {
        packageManager.getPackageInfo(packageName, 0)?.versionName
    }.orEmpty()

val Context.versionCode: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0L)
        ).longVersionCode
    } else {
        packageManager.getPackageInfo(packageName, 0)?.longVersionCode
    }
        ?.toString()
        .orEmpty()

@Composable
fun appVersion(): String {
    return if (LocalInspectionMode.current) {
        "1.0.0"
    } else {
        val context = LocalContext.current
        remember(context) { context.appVersion }
    }
}

@Composable
fun versionCode(): String {
    return if (LocalInspectionMode.current) {
        "1"
    } else {
        val context = LocalContext.current
        remember(context) { context.versionCode }
    }
}
