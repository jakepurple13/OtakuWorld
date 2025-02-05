package com.programmersbox.uiviews.presentation.components.imageloaders

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
    placeHolder: Int,
    error: Int = placeHolder,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    CustomGlideImage(
        imageUrl = imageUrl,
        name = name,
        modifier = modifier,
        headers = headers,
        placeHolder = placeHolder,
        error = error,
        contentScale = contentScale,
    )
}

@Composable
fun ImageLoaderChoice(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    headers: Map<String, Any> = emptyMap(),
    placeHolder: Painter,
    error: Painter = placeHolder,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    CustomGlideImage(
        imageUrl = imageUrl,
        name = name,
        modifier = modifier,
        headers = headers,
        placeHolder = placeHolder,
        error = error,
        contentScale = contentScale,
    )
}