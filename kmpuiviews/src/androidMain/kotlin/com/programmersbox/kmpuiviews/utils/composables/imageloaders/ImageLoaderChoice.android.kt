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
import com.bumptech.glide.load.model.Headers
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.glide.GlideImageState

// Includes Referer and Origin in the cache key so that header-gated images (e.g., hotlink-protected
// CDNs) are cached per-key rather than all sharing the same URL-only cache entry.
private class HeaderAwareGlideUrl(url: String, headers: Map<String, String>) :
    GlideUrl(url, Headers { headers }) {

    private val cacheKey = buildString {
        append(url)
        headers.entries
            .filter { (k, _) -> k.lowercase() in CACHE_KEY_HEADERS }
            .sortedBy { it.key }
            .forEach { (k, v) -> append("|$k=$v") }
    }

    override fun getCacheKey(): String = cacheKey

    companion object {
        private val CACHE_KEY_HEADERS = setOf("referer", "origin")
    }
}

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
    val url = remember(imageUrl, headers) {
        try {
            HeaderAwareGlideUrl(imageUrl, headers.mapValues { it.value.toString() })
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    GlideImage(
        imageModel = { url },
        imageOptions = ImageOptions(
            contentScale = contentScale,
            contentDescription = name,
            colorFilter = colorFilter,
        ),
        onImageStateChanged = {
            if (it is GlideImageState.Success) {
                it.imageBitmap?.let(onImageSet)
            }
        },
        loading = { Image(painter = placeHolder(), contentDescription = name) },
        failure = { Image(painter = onError(), contentDescription = name) },
        modifier = modifier,
    )
}
