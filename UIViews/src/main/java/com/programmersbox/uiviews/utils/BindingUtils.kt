package com.programmersbox.uiviews.utils

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.BindingAdapter
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.helpfulutils.changeDrawableColor
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.SwatchInfo
import kotlin.properties.Delegates

@BindingAdapter("coverImage", "logoId")
fun loadImage(view: ImageView, imageUrl: String?, logoId: Int) {
    Glide.with(view)
        .load(imageUrl)
        .override(360, 480)
        .placeholder(logoId)
        .error(logoId)
        .fallback(logoId)
        .transform(RoundedCorners(15))
        .into(view)
}

@BindingAdapter("otherNames")
fun otherNames(view: TextView, names: List<String>?) {
    view.text = names?.joinToString("\n\n")
}

@BindingAdapter("genreList", "swatch")
fun loadGenres(view: ChipGroup, genres: List<String>?, swatchInfo: SwatchInfo?) {
    view.removeAllViews()
    genres?.forEach {
        view.addView(Chip(view.context).apply {
            text = it
            isCheckable = false
            isClickable = false
            swatchInfo?.rgb?.let { setTextColor(it) }
            swatchInfo?.bodyColor?.let { chipBackgroundColor = ColorStateList.valueOf(it) }
        })
    }
}

@BindingAdapter("toolbarColors")
fun toolbarColors(view: Toolbar, swatchInfo: SwatchInfo?) {
    swatchInfo?.titleColor?.let {
        view.setTitleTextColor(it)
        view.navigationIcon?.changeDrawableColor(it)
        view.setSubtitleTextColor(it)
        view.overflowIcon?.changeDrawableColor(it)
    }
    swatchInfo?.rgb?.let { view.setBackgroundColor(it) }
}

@BindingAdapter("collapsingToolbarColors")
fun collapsingToolbarColors(view: CollapsingToolbarLayout, swatchInfo: SwatchInfo?) {
    swatchInfo?.titleColor?.let { view.setCollapsedTitleTextColor(it) }
    swatchInfo?.rgb?.let {
        view.setBackgroundColor(it)
        //view.setExpandedTitleColor(it)
    }
}

@BindingAdapter("titleColor")
fun titleColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("bodyColor")
fun bodyColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("linkColor")
fun linkColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setLinkTextColor(it) }
}

@BindingAdapter("optionTint")
fun optionTint(view: MaterialButton, swatchInfo: SwatchInfo?) {
    swatchInfo?.rgb?.let { view.strokeColor = ColorStateList.valueOf(it) }
}

@BindingAdapter("checkedButtonTint")
fun buttonTint(view: CheckBox, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.buttonTintList = ColorStateList.valueOf(it) }
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("startButtonColor")
fun startButtonColor(view: MaterialButton, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.iconTint = ColorStateList.valueOf(it) }
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
    swatchInfo?.bodyColor?.let { view.strokeColor = ColorStateList.valueOf(it) }
}

@BindingAdapter("uploadedText")
fun uploadedText(view: TextView, chapterModel: ChapterModel) {
    /*if (
        chapterModel.uploadedTime != null &&
        chapterModel.uploadedTime?.isDateBetween(System.currentTimeMillis() - 8.days.inMilliseconds.toLong(), System.currentTimeMillis()) == true
    ) {
        view.setTimeAgo(chapterModel.uploadedTime!!, showSeconds = true, autoUpdate = false)
    } else {

    }*/
    view.text = chapterModel.uploaded
}

@DslMarker
annotation class GlideMarker

fun <T> RequestBuilder<T>.into(target: CustomTargetBuilder<T>.() -> Unit) = into(CustomTargetBuilder<T>().apply(target).build())

class CustomTargetBuilder<T> internal constructor() {

    private var resourceReady: (T, Transition<in T>?) -> Unit by Delegates.notNull()

    @GlideMarker
    fun resourceReady(block: (image: T, transition: Transition<in T>?) -> Unit) = run { resourceReady = block }

    private var loadCleared: (Drawable?) -> Unit = {}

    @GlideMarker
    fun loadCleared(block: (placeHolder: Drawable?) -> Unit) = run { loadCleared = block }

    fun build() = object : CustomTarget<T>() {
        override fun onLoadCleared(placeholder: Drawable?) = loadCleared(placeholder)
        override fun onResourceReady(resource: T, transition: Transition<in T>?) = resourceReady(resource, transition)
    }

}

abstract class DragSwipeGlideAdapter<T, VH : RecyclerView.ViewHolder, Model>(
    dataList: MutableList<T> = mutableListOf()
) : DragSwipeAdapter<T, VH>(dataList), ListPreloader.PreloadModelProvider<Model> {

    protected abstract val fullRequest: RequestBuilder<Drawable>
    protected abstract val thumbRequest: RequestBuilder<Drawable>
    protected abstract val itemToModel: (T) -> Model

    @NonNull
    override fun getPreloadItems(position: Int): List<Model> = dataList.subList(position, position + 1).map(itemToModel).toList()

    @NonNull
    override fun getPreloadRequestBuilder(item: Model): RequestBuilder<Drawable?>? = fullRequest.thumbnail(thumbRequest.load(item)).load(item)
}

fun Drawable.getPalette() = Palette.from(toBitmap()).generate()
fun Bitmap.getPalette() = Palette.from(this).generate()

fun Int.getPaletteFromColor(): Palette {
    val b = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
    Canvas(b).drawColor(this@getPaletteFromColor)
    return b.getPalette()
}

fun String.getPaletteFromHexColor(): Palette = Color.parseColor(this@getPaletteFromHexColor).getPaletteFromColor()

fun Int.toColorDrawable() = ColorDrawable(this)
fun String.toColorDrawable() = ColorDrawable(Color.parseColor(this))

var LottieAnimationView.checked: Boolean
    get() = progress == 1f
    set(value) = check(value)

fun LottieAnimationView.check(checked: Boolean) {
    val endProgress = if (checked) 1f else 0f
    val animator = ValueAnimator.ofFloat(progress, endProgress).apply {
        addUpdateListener { animation: ValueAnimator -> progress = animation.animatedValue as Float }
    }
    animator.start()
}

fun LottieAnimationView.changeTint(@ColorInt newColor: Int) =
    addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_ATOP) }
