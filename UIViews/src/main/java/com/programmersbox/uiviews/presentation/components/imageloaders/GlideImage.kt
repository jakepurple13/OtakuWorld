package com.programmersbox.uiviews.presentation.components.imageloaders

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.load.model.GlideUrl
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun CustomGlideImage(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    headers: Map<String, Any> = emptyMap(),
    placeHolder: Int,
    error: Int = placeHolder,
    contentScale: ContentScale = ContentScale.FillBounds,
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
        loading = {
            Image(painter = painterResource(placeHolder), contentDescription = name)
        },
        failure = {
            Image(painter = painterResource(error), contentDescription = name)
        },
        modifier = modifier
    )
}

@Composable
fun CustomGlideImage(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    headers: Map<String, Any> = emptyMap(),
    placeHolder: Painter,
    error: Painter = placeHolder,
    contentScale: ContentScale = ContentScale.FillBounds,
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
        loading = {
            Image(painter = placeHolder, contentDescription = name)
        },
        failure = {
            Image(painter = error, contentDescription = name)
        },
        modifier = modifier
    )
}