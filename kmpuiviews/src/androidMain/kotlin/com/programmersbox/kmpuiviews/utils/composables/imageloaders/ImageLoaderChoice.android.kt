package com.programmersbox.kmpuiviews.utils.composables.imageloaders

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.load.model.GlideUrl
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.glide.GlideImageState
import org.koin.compose.koinInject

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
            colorFilter = colorFilter
        ),
        onImageStateChanged = {
            if (it is GlideImageState.Success) {
                it.imageBitmap?.let(onImageSet)
            }
        },
        loading = { Image(painter = placeHolder(), contentDescription = name) },
        failure = { Image(painter = onError(), contentDescription = name) },
        modifier = modifier
    )
}