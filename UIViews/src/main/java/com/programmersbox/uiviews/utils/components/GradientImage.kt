package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun GradientImage(
    model: Any,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    error: Painter? = placeholder,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.FillBounds,
    blur: Dp = 70.dp,
    alpha: Float = .5f,
    saturation: Float = 3f,
    scaleX: Float = 1.5f,
    scaleY: Float = 1.5f
) {
    Box {
        AsyncImage(
            model = model,
            placeholder = placeholder,
            error = error,
            contentScale = contentScale,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }),
            modifier = Modifier
                .scale(scaleX, scaleY)
                .blur(blur, BlurredEdgeTreatment.Unbounded)
                .alpha(alpha)
                .then(modifier)
        )

        AsyncImage(
            model = model,
            placeholder = placeholder,
            error = error,
            contentScale = contentScale,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}