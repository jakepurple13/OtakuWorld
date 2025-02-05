package com.programmersbox.uiviews.presentation.components.imageloaders

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder

@Composable
fun CoilImage(
    imageUrl: String,
    headers: Map<String, Any> = emptyMap(),
    name: String,
    placeHolder: Int,
    modifier: Modifier = Modifier,
    error: Int = placeHolder,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .httpHeaders(
                NetworkHeaders
                    .Builder()
                    .apply { headers.forEach { add(it.key, it.value.toString()) } }
                    .build()
            )
            .placeholder(placeHolder)
            .error(error)
            .build(),
        contentScale = contentScale,
        contentDescription = name,
        modifier = modifier
    )
}