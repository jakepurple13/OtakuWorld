package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import io.kamel.core.Resource
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun GradientImage(
    model: Any,
    placeholder: Painter,
    modifier: Modifier = Modifier,
    error: Painter = placeholder,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.FillBounds,
    blur: Dp = 70.dp,
    alpha: Float = .5f,
    saturation: Float = 3f,
    scaleX: Float = 1.5f,
    scaleY: Float = 1.5f,
) {
    Box {
        when (val painter = asyncPainterResource(data = model)) {
            is Resource.Success -> {
                KamelImage(
                    resource = { painter },
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }),
                    modifier = Modifier
                        .blurGradient(blur, alpha, scaleX, scaleY)
                        .then(modifier)
                )

                KamelImage(
                    resource = { painter },
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = modifier
                )
            }

            else -> {
                Image(
                    painter = error,
                    contentScale = contentScale,
                    contentDescription = contentDescription,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun CoilGradientImage(
    model: AsyncImagePainter,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.FillBounds,
    blur: Dp = 70.dp,
    alpha: Float = .5f,
    saturation: Float = 3f,
    scaleX: Float = 1.5f,
    scaleY: Float = 1.5f,
) {
    Box {
        if (model.state is AsyncImagePainter.State.Success) {
            Image(
                painter = model,
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }),
                modifier = Modifier
                    .blurGradient(blur, alpha, scaleX, scaleY)
                    .then(modifier)
            )
        }

        Image(
            painter = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GlideGradientImage(
    model: Any?,
    placeholder: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.FillBounds,
    blur: Dp = 70.dp,
    alpha: Float = .5f,
    saturation: Float = 3f,
    scaleX: Float = 1.5f,
    scaleY: Float = 1.5f,
) {

    Box {
        GlideImage(
            model = model,
            loading = placeholder(placeholder),
            failure = placeholder(placeholder),
            transition = CrossFade,
            contentDescription = contentDescription,
            contentScale = contentScale,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }),
            modifier = Modifier
                .blurGradient(blur, alpha, scaleX, scaleY)
                .then(modifier)
        )
        GlideImage(
            model = model,
            transition = CrossFade,
            loading = placeholder(placeholder),
            failure = placeholder(placeholder),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

fun Modifier.blurGradient(
    blur: Dp = 70.dp,
    alpha: Float = .5f,
    scaleX: Float = 1.5f,
    scaleY: Float = 1.5f,
) = scale(scaleX, scaleY)
    .blur(blur, BlurredEdgeTreatment.Unbounded)
    .alpha(alpha)

fun Modifier.blurGradientV2(
    blur: Dp = 70.dp,
    alpha: Float = .5f,
    scaleX: Float = 1.5f,
    scaleY: Float = 1.5f,
) = graphicsLayer {
    this.scaleX = scaleX
    this.scaleY = scaleY
    this.alpha = alpha
    this.renderEffect = BlurEffect(blur.value, blur.value, TileMode.Decal)
}

fun Modifier.blurGradient(
    blur: Dp = 70.dp,
    alpha: Float = .5f,
    scale: Float = 1.5f,
) = scale(scale)
    .blur(blur, BlurredEdgeTreatment.Unbounded)
    .alpha(alpha)
