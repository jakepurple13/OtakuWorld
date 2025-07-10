package com.programmersbox.kmpuiviews.utils.composables.imageloaders

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

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
    CustomKamelImage(
        imageUrl = imageUrl,
        name = name,
        modifier = modifier,
        headers = headers,
        placeHolder = placeHolder,
        onError = onError,
        contentScale = contentScale,
    )
}