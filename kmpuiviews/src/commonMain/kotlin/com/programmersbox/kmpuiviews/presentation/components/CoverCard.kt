package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.composables.modifiers.bounceClick
import com.programmersbox.kmpuiviews.utils.composables.modifiers.combineClickableWithIndication
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuImageElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuTitleElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.customSharedElement
import com.programmersbox.kmpuiviews.zoomOverlay

@Composable
fun M3CoverCard(
    imageUrl: String,
    name: String,
    placeHolder: @Composable () -> Painter,
    modifier: Modifier = Modifier,
    error: @Composable () -> Painter = placeHolder,
    headers: Map<String, Any> = emptyMap(),
    onLongPress: ((ComponentState) -> Unit)? = null,
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
    colorFilter: ColorFilter? = null,
    onClick: () -> Unit = {},
) {
    @Composable
    fun CustomSurface(modifier: Modifier, tonalElevation: Dp, shape: Shape, content: @Composable () -> Unit) {
        onLongPress?.let {
            Surface(
                modifier = modifier.combineClickableWithIndication(it, onClick),
                tonalElevation = tonalElevation,
                shape = shape,
                content = content
            )
        } ?: Surface(
            modifier = modifier,
            tonalElevation = tonalElevation,
            shape = shape,
            onClick = onClick,
            content = content
        )
    }
    CustomSurface(
        modifier = modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .bounceClick(.9f)
            .customSharedElement(
                OtakuImageElement(
                    origin = imageUrl,
                    source = name
                )
            )
            .zoomOverlay(),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ImageLoaderChoice(
                imageUrl = imageUrl,
                headers = headers,
                name = name,
                placeHolder = placeHolder,
                error = error,
                contentScale = ContentScale.FillBounds,
                colorFilter = colorFilter,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            ) {
                Text(
                    name,
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .align(Alignment.BottomCenter)
                        .customSharedElement(OtakuTitleElement(name, name))
                )
            }

            favoriteIcon()
        }

    }
}

@Composable
fun M3ImageCard(
    imageUrl: String,
    name: String,
    placeHolder: @Composable () -> Painter,
    modifier: Modifier = Modifier,
    error: @Composable () -> Painter = placeHolder,
    colorFilter: ColorFilter? = null,
    headers: Map<String, Any> = emptyMap(),
) {
    Surface(
        modifier = modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .zoomOverlay(),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ImageLoaderChoice(
                imageUrl = imageUrl,
                headers = headers,
                placeHolder = placeHolder,
                error = error,
                contentScale = ContentScale.FillBounds,
                name = name,
                colorFilter = colorFilter,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            ) {
                Text(
                    name,
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun M3CoverCard(
    imageUrl: String,
    name: String,
    placeHolder: @Composable () -> Painter,
    modifier: Modifier = Modifier,
    error: @Composable () -> Painter = placeHolder,
    headers: Map<String, Any> = emptyMap(),
    colorFilter: ColorFilter? = null,
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .bounceClick(.9f)
            .zoomOverlay()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ImageLoaderChoice(
                imageUrl = imageUrl,
                contentScale = ContentScale.FillBounds,
                name = name,
                headers = headers,
                placeHolder = placeHolder,
                error = error,
                colorFilter = colorFilter,
                modifier = Modifier
                    .matchParentSize()
                    .customSharedElement(
                        OtakuImageElement(
                            origin = imageUrl,
                            source = name
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            ) {
                Text(
                    name,
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .align(Alignment.BottomCenter)
                        .customSharedElement(OtakuTitleElement(name, name))
                )
            }

            favoriteIcon()
        }
    }
}

@Composable
fun M3CoverCard2(
    imageUrl: String,
    name: String,
    placeHolder: @Composable () -> Painter,
    modifier: Modifier = Modifier,
    error: @Composable () -> Painter = placeHolder,
    headers: Map<String, Any> = emptyMap(),
    colorFilter: ColorFilter? = null,
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .zoomOverlay()
            .then(modifier),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ImageLoaderChoice(
                imageUrl = imageUrl,
                contentScale = ContentScale.FillBounds,
                name = name,
                headers = headers,
                placeHolder = placeHolder,
                error = error,
                colorFilter = colorFilter,
                modifier = Modifier
                    .matchParentSize()
                    .customSharedElement(
                        OtakuImageElement(
                            origin = imageUrl,
                            source = name
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            ) {
                Text(
                    name,
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .align(Alignment.BottomCenter)
                        .customSharedElement(OtakuTitleElement(name, name))
                )
            }

            favoriteIcon()
        }
    }
}
