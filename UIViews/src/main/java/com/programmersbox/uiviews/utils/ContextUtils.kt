package com.programmersbox.uiviews.utils

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
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
import androidx.core.graphics.createBitmap

fun Context.openInCustomChromeBrowser(url: Uri, build: CustomTabsIntent.Builder.() -> Unit = {}) = CustomTabsIntent.Builder()
    .setExitAnimations(this, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    .apply(build)
    .build()
    .launchUrl(this, url)

fun Context.openInCustomChromeBrowser(url: String, build: CustomTabsIntent.Builder.() -> Unit = {}) = openInCustomChromeBrowser(Uri.parse(url), build)

fun View.toolTipText(text: CharSequence) {
    tooltipText = text
}

fun View.toolTipText(@StringRes stringId: Int) = toolTipText(context.getString(stringId))

fun Bitmap.glowEffect(glowRadius: Int, glowColor: Int): Bitmap {
    val margin = 24
    val halfMargin = margin / 2f
    val alpha = extractAlpha()
    val out = createBitmap(width + margin, height + margin)
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

val LocalGenericInfo = staticCompositionLocalOf<GenericInfo> { error("No Info") }