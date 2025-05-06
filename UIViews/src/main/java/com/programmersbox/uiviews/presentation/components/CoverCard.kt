package com.programmersbox.uiviews.presentation.components

import android.graphics.drawable.Drawable
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.uiviews.presentation.components.imageloaders.ImageLoaderChoice
import com.programmersbox.uiviews.utils.bounceClick
import com.programmersbox.uiviews.utils.combineClickableWithIndication
import com.programmersbox.uiviews.utils.sharedelements.OtakuImageElement
import com.programmersbox.uiviews.utils.sharedelements.OtakuTitleElement
import com.programmersbox.uiviews.utils.sharedelements.customSharedElement
import com.skydoves.landscapist.rememberDrawablePainter
import me.saket.telephoto.zoomable.rememberZoomablePeekOverlayState
import me.saket.telephoto.zoomable.zoomablePeekOverlay

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun M3CoverCard(
    imageUrl: String,
    name: String,
    placeHolder: Int,
    modifier: Modifier = Modifier,
    error: Int = placeHolder,
    headers: Map<String, Any> = emptyMap(),
    onLongPress: ((ComponentState) -> Unit)? = null,
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
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
            .zoomablePeekOverlay(state = rememberZoomablePeekOverlayState()),
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
    placeHolder: Int,
    modifier: Modifier = Modifier,
    error: Int = placeHolder,
    headers: Map<String, Any> = emptyMap(),
) {
    Surface(
        modifier = modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .zoomablePeekOverlay(state = rememberZoomablePeekOverlayState()),
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
    placeHolder: Drawable?,
    modifier: Modifier = Modifier,
    error: Drawable? = placeHolder,
    headers: Map<String, Any> = emptyMap(),
    onLongPress: ((ComponentState) -> Unit)? = null,
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
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
            .zoomablePeekOverlay(state = rememberZoomablePeekOverlayState()),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
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
                placeHolder = rememberDrawablePainter(placeHolder),
                error = rememberDrawablePainter(error),
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
fun M3CoverCard(
    imageUrl: String,
    name: String,
    placeHolder: Drawable?,
    modifier: Modifier = Modifier,
    error: Drawable? = placeHolder,
    headers: Map<String, Any> = emptyMap(),
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .bounceClick(.9f)
            .zoomablePeekOverlay(state = rememberZoomablePeekOverlayState()),
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
                placeHolder = rememberDrawablePainter(placeHolder),
                error = rememberDrawablePainter(error),
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
    placeHolder: Int,
    modifier: Modifier = Modifier,
    error: Int = placeHolder,
    headers: Map<String, Any> = emptyMap(),
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .zoomablePeekOverlay(state = rememberZoomablePeekOverlayState())
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