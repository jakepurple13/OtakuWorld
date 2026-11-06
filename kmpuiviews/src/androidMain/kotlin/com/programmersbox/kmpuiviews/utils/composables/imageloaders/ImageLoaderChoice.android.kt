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
        // Decode as a Bitmap, never a GifDrawable. clearTarget can release a request whose image
        // is still on screen (landscapist re-keys its target on every recomposition), and a
        // recycled GifDrawable shares a frame loader whose frame is then null, so drawing it or
        // calling toBitmap() on it throws an NPE. Animated GIF covers show their first frame.
        glideRequestType = GlideRequestType.BITMAP,
        // Release the request when this leaves composition so Glide moves the bitmap into its
        // LRU memory cache. Without this it stays a weakly-held "active resource" and is lost on
        // GC, so returning to a screen (e.g. Recent) reloads every cover.
        clearTarget = true,
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