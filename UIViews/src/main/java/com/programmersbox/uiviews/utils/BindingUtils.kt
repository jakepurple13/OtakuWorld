package com.programmersbox.uiviews.utils

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.programmersbox.dragswipe.DragSwipeAdapter
import kotlin.properties.Delegates

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
