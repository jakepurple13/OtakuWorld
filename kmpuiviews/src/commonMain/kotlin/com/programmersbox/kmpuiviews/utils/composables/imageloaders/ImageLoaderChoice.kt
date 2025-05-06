package com.programmersbox.kmpuiviews.utils.composables.imageloaders

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

@Composable
fun ImageLoaderChoice(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    headers: Map<String, Any> = emptyMap(),
    placeHolder: @Composable () -> Painter,
    error: @Composable () -> Painter = placeHolder,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    /*CustomKamelImage(
        imageUrl = imageUrl,
        name = name,
        modifier = modifier,
        headers = headers,
        placeHolder = placeHolder,
        onError = error,
        contentScale = contentScale,
    )*/

    CustomImageChoice(
        imageUrl = imageUrl,
        name = name,
        modifier = modifier,
        headers = headers,
        placeHolder = placeHolder,
        onError = error,
        contentScale = contentScale,
    )
}

@Composable
expect fun CustomImageChoice(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    headers: Map<String, Any> = emptyMap(),
    placeHolder: @Composable () -> Painter,
    error: @Composable () -> Painter = placeHolder,
    contentScale: ContentScale = ContentScale.FillBounds,
)