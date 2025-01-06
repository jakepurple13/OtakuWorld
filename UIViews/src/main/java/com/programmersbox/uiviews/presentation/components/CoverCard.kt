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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.bounceClick
import com.programmersbox.uiviews.utils.combineClickableWithIndication
import com.programmersbox.uiviews.utils.sharedelements.OtakuImageElement
import com.programmersbox.uiviews.utils.sharedelements.OtakuTitleElement
import com.programmersbox.uiviews.utils.sharedelements.customSharedElement

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
            ),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .lifecycle(LocalLifecycleOwner.current)
                    .apply { headers.forEach { addHeader(it.key, it.value.toString()) } }
                    .crossfade(true)
                    .placeholder(placeHolder)
                    .error(error)
                    .build(),
                contentScale = ContentScale.FillBounds,
                contentDescription = name,
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
        modifier = modifier.size(
            ComposableUtils.IMAGE_WIDTH,
            ComposableUtils.IMAGE_HEIGHT
        ),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .lifecycle(LocalLifecycleOwner.current)
                    .apply { headers.forEach { addHeader(it.key, it.value.toString()) } }
                    .crossfade(true)
                    .placeholder(placeHolder)
                    .error(error)
                    .build(),
                contentScale = ContentScale.FillBounds,
                contentDescription = name,
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
            .bounceClick(.9f),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .lifecycle(LocalLifecycleOwner.current)
                    .apply { headers.forEach { addHeader(it.key, it.value.toString()) } }
                    .crossfade(true)
                    .placeholder(placeHolder)
                    .error(error)
                    .build(),
                contentScale = ContentScale.FillBounds,
                contentDescription = name,
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
            .bounceClick(.9f),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .lifecycle(LocalLifecycleOwner.current)
                    .apply { headers.forEach { addHeader(it.key, it.value.toString()) } }
                    .crossfade(true)
                    .placeholder(placeHolder)
                    .error(error)
                    .build(),
                contentScale = ContentScale.FillBounds,
                contentDescription = name,
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