package com.programmersbox.kmpuiviews.utils.composables.imageloaders

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
fun CustomCoilImage(
    imageUrl: String,
    headers: Map<String, Any> = emptyMap(),
    name: String,
    placeHolder: Int,
    modifier: Modifier = Modifier,
    error: Int = placeHolder,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    /*AsyncImage(
        model = ImageRequest.Builder()
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
    )*/
}