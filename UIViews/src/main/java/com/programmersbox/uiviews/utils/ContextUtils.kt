package com.programmersbox.uiviews.utils

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.text.Spannable
import android.text.Spanned
import android.text.method.TransformationMethod
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.*
import com.programmersbox.gsonutils.sharedPrefNotNullObjectDelegate
import com.programmersbox.gsonutils.sharedPrefObjectDelegate
import com.programmersbox.helpfulutils.sharedPrefDelegate
import com.programmersbox.helpfulutils.sharedPrefNotNullDelegate
import com.programmersbox.models.ChapterModel
import com.programmersbox.uiviews.GenericInfo
import io.reactivex.subjects.BehaviorSubject
import java.lang.reflect.Type

var Context.currentService: String? by sharedPrefObjectDelegate(null)

var Context.shouldCheck: Boolean by sharedPrefNotNullObjectDelegate(true)

var Context.lastUpdateCheck: Long? by sharedPrefDelegate(null)
var Context.lastUpdateCheckEnd: Long? by sharedPrefDelegate(null)

val updateCheckPublish = BehaviorSubject.create<Long>()
val updateCheckPublishEnd = BehaviorSubject.create<Long>()

var Context.batteryAlertPercent: Int by sharedPrefNotNullDelegate(20)

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