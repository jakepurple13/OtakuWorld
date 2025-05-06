@file:OptIn(ExperimentalGlideComposeApi::class)

package com.programmersbox.uiviews.utils

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.uiviews.presentation.components.BannerBox
import com.programmersbox.uiviews.presentation.components.GlideGradientImage

@Composable
fun M3PlaceHolderCoverCard(placeHolder: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(
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
                    .m3placeholder(
                        true,
                        highlight = PlaceholderHighlight.shimmer()
                    )
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
                        .m3placeholder(
                            true,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtakuBannerBox(
    placeholder: Int,
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    content: @Composable BannerScope.() -> Unit,
) {
    var itemInfo by remember { mutableStateOf<KmpItemModel?>(null) }

    val bannerScope = BannerScope { itemModel -> itemInfo = itemModel }

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
                        GlideGradientImage(
                            model = itemInfo?.imageUrl.orEmpty(),
                            placeholder = placeholder,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                .clip(MaterialTheme.shapes.small)
                        )
                    },
                    overlineContent = { Text(itemInfo?.source?.serviceName.orEmpty()) },
                    headlineContent = { Text(itemInfo?.title.orEmpty()) },
                    supportingContent = {
                        Text(
                            itemInfo?.description.orEmpty(),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 5
                        )
                    }
                )
            }
        },
        content = { bannerScope.content() }
    )
}

fun interface BannerScope {
    //TODO: Maybe add a modifier into here for onLongClick?
    fun newItemModel(itemModel: KmpItemModel?)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CustomBannerBox(
    bannerContent: @Composable BoxScope.(T?) -> Unit,
    modifier: Modifier = Modifier,
    showBanner: Boolean = false,
    content: @Composable CustomBannerScope<T>.() -> Unit,
) {
    var itemInfo by remember { mutableStateOf<T?>(null) }

    val bannerScope = remember {
        object : CustomBannerScope<T> {
            override fun newItem(item: T?) {
                itemInfo = item
            }
        }
    }

    BannerBox(
        modifier = modifier,
        showBanner = showBanner,
        banner = {
            Surface(
                shape = MaterialTheme.shapes.medium.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp)),
                tonalElevation = 4.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .animateContentSize(),
            ) {
                bannerContent(itemInfo)
            }
        },
        content = { bannerScope.content() }
    )
}

interface CustomBannerScope<T> {
    fun newItem(item: T?)
}