package com.programmersbox.uiviews.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.placeholder.material.placeholder
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.utils.components.BannerBox

object ComposableUtils {
    const val IMAGE_WIDTH_PX = 360
    const val IMAGE_HEIGHT_PX = 480
    val IMAGE_WIDTH @Composable get() = with(LocalDensity.current) { IMAGE_WIDTH_PX.toDp() }
    val IMAGE_HEIGHT @Composable get() = with(LocalDensity.current) { IMAGE_HEIGHT_PX.toDp() }
}

@Composable
fun M3CoverCard(
    imageUrl: String,
    name: String,
    placeHolder: Int,
    modifier: Modifier = Modifier,
    error: Int = placeHolder,
    headers: Map<String, Any> = emptyMap(),
    onLongPress: (ComponentState) -> Unit = {},
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .bounceClick(.9f)
            .combineClickableWithIndication(onLongPress, onClick)
            .then(modifier),
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
                    .size(ComposableUtils.IMAGE_WIDTH_PX, ComposableUtils.IMAGE_HEIGHT_PX)
                    .build(),
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

            favoriteIcon()
        }

    }
}

@Composable
fun M3PlaceHolderCoverCard(placeHolder: Int) {
    val placeholderColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface)
        .copy(0.1f)
        .compositeOver(MaterialTheme.colorScheme.surface)

    Surface(
        modifier = Modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            ),
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {

        Box {
            Image(
                painter = painterResource(placeHolder),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .placeholder(true, color = placeholderColor)
                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
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
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    "",
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .placeholder(true, color = placeholderColor)
                        .align(Alignment.BottomCenter)
                )
            }
        }

    }
}

@Composable
fun M3OtakuBannerBox(
    placeholder: Int,
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    content: @Composable BoxScope.(itemInfo: MutableState<ItemModel?>) -> Unit
) {
    val itemInfo = remember { mutableStateOf<ItemModel?>(null) }

    BannerBox(
        modifier = modifier,
        showBanner = showBanner,
        banner = {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter),
                shape = MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp)),
                tonalElevation = 4.dp,
                shadowElevation = 10.dp
            ) {
                ListItem(
                    leadingContent = {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(itemInfo.value?.imageUrl.orEmpty())
                                .lifecycle(LocalLifecycleOwner.current)
                                .crossfade(true)
                                .placeholder(placeholder)
                                .error(placeholder)
                                .size(ComposableUtils.IMAGE_WIDTH_PX, ComposableUtils.IMAGE_HEIGHT_PX)
                                .build(),
                            contentDescription = itemInfo.value?.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    },
                    overlineContent = { Text(itemInfo.value?.source?.serviceName.orEmpty()) },
                    headlineContent = { Text(itemInfo.value?.title.orEmpty()) },
                    supportingContent = {
                        Text(
                            itemInfo.value?.description.orEmpty(),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 5
                        )
                    }
                )
            }
        },
        content = { content(itemInfo) }
    )
}