package com.programmersbox.kmpuiviews.utils.composables.imageloaders

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.client.request.header

@Composable
fun CustomKamelImage(
    imageUrl: String,
    headers: Map<String, Any> = emptyMap(),
    name: String,
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Painter,
    onError: @Composable () -> Painter,
    contentScale: ContentScale = ContentScale.FillBounds,
) {
    KamelImage(
        resource = {
            asyncPainterResource(imageUrl) {
                requestBuilder {
                    headers.forEach { (t, u) -> header(t, u) }
                }
            }
        },
        onLoading = {
            Image(
                painter = placeHolder(),
                contentDescription = name,
            )
        },
        onFailure = {
            Image(
                painter = onError(),
                contentDescription = name,
            )
        },
        contentDescription = name,
        contentScale = contentScale,
        modifier = modifier
    )
}