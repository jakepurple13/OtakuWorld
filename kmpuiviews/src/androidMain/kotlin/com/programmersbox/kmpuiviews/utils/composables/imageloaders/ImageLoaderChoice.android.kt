package com.programmersbox.kmpuiviews.utils.composables.imageloaders

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.crossfade.CrossfadePlugin
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.glide.GlideImageState
import com.skydoves.landscapist.glide.GlideRequestType

@Composable
actual fun CustomImageChoice(
    imageUrl: String,
    name: String,
    modifier: Modifier,
    headers: Map<String, Any>,
    placeHolder: @Composable (() -> Painter),
    onError: @Composable (() -> Painter),
    contentScale: ContentScale,
    colorFilter: ColorFilter?,
    onImageSet: (ImageBitmap) -> Unit,
) {
    val url = remember(imageUrl) {
        try {
            // LazyHeaders has value equality. GlideUrl.equals compares headers, so a lambda
            // Headers made every memory-cache key unique and nothing was ever a cache hit.
            GlideUrl(
                imageUrl,
                LazyHeaders.Builder()
                    .apply { headers.forEach { (key, value) -> addHeader(key, value.toString()) } }
                    .build()
            )
        } catch (_: IllegalArgumentException) {
            ""
        }
    }

    GlideImage(
        imageModel = { url },
        // Decode as a Bitmap, never a GifDrawable. Animated GIF covers show their first frame.
        glideRequestType = GlideRequestType.BITMAP,
        // Do not pass clearTarget = true. Landscapist 2.13.2 builds a new FlowCustomTarget (no
        // equals) on every recomposition and keys remember() on it, so clearTarget calls
        // Glide.clear() on a request whose image is still on screen. Glide then hands the bitmap
        // to its BitmapPool, which recycles it when rejected or evicted, and the next draw throws
        // "Canvas: trying to use a recycled bitmap" (and, for GifDrawables, an NPE).
        imageOptions = ImageOptions(
            contentScale = contentScale,
            contentDescription = name,
            colorFilter = colorFilter
        ),
        onImageStateChanged = {
            if (it is GlideImageState.Success) {
                it.imageBitmap?.let(onImageSet)
            }
        },
        component = rememberImageComponent {
            +CrossfadePlugin()
        },
        loading = { Image(painter = placeHolder(), contentDescription = name) },
        failure = { Image(painter = onError(), contentDescription = name) },
        modifier = modifier
    )
}