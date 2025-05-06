package com.programmersbox.kmpuiviews.utils.composables.imageloaders

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.load.model.GlideUrl
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
actual fun CustomImageChoice(
    imageUrl: String,
    name: String,
    modifier: Modifier,
    headers: Map<String, Any>,
    placeHolder: @Composable (() -> Painter),
    error: @Composable (() -> Painter),
    contentScale: ContentScale,
) {
    val url = remember(imageUrl) {
        try {
            GlideUrl(imageUrl) { headers.mapValues { it.value.toString() } }
        } catch (_: IllegalArgumentException) {
            ""
        }
    }

    GlideImage(
        imageModel = { url },
        imageOptions = ImageOptions(
            contentScale = contentScale,
            contentDescription = name,
        ),
        loading = { Image(painter = placeHolder(), contentDescription = name) },
        failure = { Image(painter = error(), contentDescription = name) },
        modifier = modifier
    )
}