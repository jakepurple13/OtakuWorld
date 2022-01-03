package com.programmersbox.uiviews.utils

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.accompanist.placeholder.material.placeholder
import com.programmersbox.models.ItemModel
import com.skydoves.landscapist.glide.GlideImage
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

object ComposableUtils {
    val IMAGE_WIDTH @Composable get() = with(LocalDensity.current) { 360.toDp() }
    val IMAGE_HEIGHT @Composable get() = with(LocalDensity.current) { 480.toDp() }
}

@ExperimentalMaterialApi
@Composable
fun CoverCard(
    modifier: Modifier = Modifier,
    imageUrl: String,
    name: String,
    placeHolder: Int,
    error: Int = placeHolder,
    onLongPress: (ComponentState) -> Unit = {},
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(4.dp)
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .combineClickableWithIndication(onLongPress, onClick)
            .then(modifier)
    ) {
        Box {
            GlideImage(
                imageModel = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                requestBuilder = {
                    Glide.with(LocalView.current)
                        .asDrawable()
                        //.override(360, 480)
                        .placeholder(placeHolder)
                        .error(error)
                        .fallback(placeHolder)
                        .transform(RoundedCorners(5))
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                loading = {
                    Image(
                        bitmap = AppCompatResources.getDrawable(context, placeHolder)!!.toBitmap().asImageBitmap(),
                        contentDescription = name,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    )
                },
                failure = {
                    Image(
                        bitmap = AppCompatResources.getDrawable(context, error)!!.toBitmap().asImageBitmap(),
                        contentDescription = name,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    )
                }
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
                    name,
                    style = MaterialTheme
                        .typography
                        .body1
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                )
            }

            favoriteIcon()
        }

    }
}

@ExperimentalMaterialApi
@Composable
fun PlaceHolderCoverCard(placeHolder: Int) {
    Card(
        modifier = Modifier
            .padding(5.dp)
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
    ) {

        Box {
            Image(
                painter = painterResource(placeHolder),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .placeholder(true)
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
                        .body1
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .placeholder(true)
                        .align(Alignment.BottomCenter)
                )
            }
        }

    }
}

@ExperimentalMaterialApi
@Composable
fun OtakuBannerBox(
    showBanner: Boolean = false,
    placeholder: Int,
    content: @Composable BoxScope.(itemInfo: MutableState<ItemModel?>) -> Unit
) {
    val context = LocalContext.current
    val itemInfo = remember { mutableStateOf<ItemModel?>(null) }
    val placeHolderImage = remember {
        AppCompatResources
            .getDrawable(context, placeholder)!!
            .toBitmap().asImageBitmap()
    }

    BannerBox2(
        showBanner = showBanner,
        bannerSize = ComposableUtils.IMAGE_HEIGHT + 20.dp,
        banner = {
            Card(modifier = Modifier.align(Alignment.TopCenter)) {
                ListItem(
                    icon = {
                        GlideImage(
                            imageModel = itemInfo.value?.imageUrl.orEmpty(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                            loading = {
                                Image(
                                    bitmap = placeHolderImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                )
                            },
                            failure = {
                                Image(
                                    bitmap = placeHolderImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                )
                            }
                        )
                    },
                    overlineText = { Text(itemInfo.value?.source?.serviceName.orEmpty()) },
                    text = { Text(itemInfo.value?.title.orEmpty()) },
                    secondaryText = {
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

@ExperimentalMaterialApi
@Composable
fun M3CoverCard(
    modifier: Modifier = Modifier,
    imageUrl: String,
    name: String,
    placeHolder: Int,
    error: Int = placeHolder,
    onLongPress: (ComponentState) -> Unit = {},
    favoriteIcon: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    androidx.compose.material3.Surface(
        modifier = Modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
            .combineClickableWithIndication(onLongPress, onClick)
            .then(modifier),
        tonalElevation = 5.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberImagePainter(imageUrl) {
                    placeholder(AppCompatResources.getDrawable(context, placeHolder)!!)
                    error(AppCompatResources.getDrawable(context, error)!!)
                    crossfade(true)
                    lifecycle(LocalLifecycleOwner.current)
                    size(480, 360)

                },
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
                    style = M3MaterialTheme
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

@ExperimentalMaterialApi
@Composable
fun M3PlaceHolderCoverCard(placeHolder: Int) {
    val placeholderColor = androidx.compose.material3.contentColorFor(backgroundColor = M3MaterialTheme.colorScheme.surface)
        .copy(0.1f)
        .compositeOver(M3MaterialTheme.colorScheme.surface)

    androidx.compose.material3.Surface(
        modifier = Modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            ),
        tonalElevation = 5.dp,
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
                androidx.compose.material3.Text(
                    "",
                    style = M3MaterialTheme
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

@OptIn(ExperimentalCoilApi::class)
@ExperimentalMaterialApi
@Composable
fun M3OtakuBannerBox(
    showBanner: Boolean = false,
    placeholder: Int,
    content: @Composable BoxScope.(itemInfo: MutableState<ItemModel?>) -> Unit
) {
    val context = LocalContext.current
    val itemInfo = remember { mutableStateOf<ItemModel?>(null) }
    val placeHolderImage = remember {
        AppCompatResources
            .getDrawable(context, placeholder)!!
            .toBitmap().asImageBitmap()
    }

    BannerBox(
        showBanner = showBanner,
        banner = {
            androidx.compose.material3.Surface(
                modifier = Modifier.align(Alignment.TopCenter),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 5.dp,
                shadowElevation = 10.dp
            ) {
                ListItem(
                    icon = {
                        /*GlideImage(
                            imageModel = itemInfo.value?.imageUrl.orEmpty(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                            loading = {
                                Image(
                                    bitmap = placeHolderImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                )
                            },
                            failure = {
                                Image(
                                    bitmap = placeHolderImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                )
                            }
                        )*/
                        val painter = rememberImagePainter(data = itemInfo.value?.imageUrl.orEmpty())

                        when (painter.state) {
                            is ImagePainter.State.Loading -> {
                                Image(
                                    bitmap = placeHolderImage,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                )
                            }
                            is ImagePainter.State.Error -> {
                                GlideImage(
                                    imageModel = itemInfo.value?.imageUrl.orEmpty(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                                    loading = {
                                        Image(
                                            bitmap = placeHolderImage,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                        )
                                    },
                                    failure = {
                                        Image(
                                            bitmap = placeHolderImage,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                        )
                                    }
                                )
                            }
                            else -> {}
                        }

                        Image(
                            painter = painter,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                        )
                    },
                    overlineText = { androidx.compose.material3.Text(itemInfo.value?.source?.serviceName.orEmpty()) },
                    text = { androidx.compose.material3.Text(itemInfo.value?.title.orEmpty()) },
                    secondaryText = {
                        androidx.compose.material3.Text(
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