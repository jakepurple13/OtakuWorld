package com.programmersbox.otakumanager

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.programmersbox.models.InfoModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.CustomChip
import com.programmersbox.uiviews.utils.fadeInAnimation
import com.programmersbox.uiviews.utils.toComposeColor
import com.skydoves.landscapist.glide.GlideImage

@ExperimentalMaterialApi
@Composable
private fun DetailsHeader(
    model: InfoModel,
    logo: MainLogo,
    swatchInfo: SwatchInfo?,
    isFavorite: Boolean,
    favoriteClick: (Boolean) -> Unit
) {

    var descriptionVisibility by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
    ) {

        GlideImage(
            imageModel = model.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            requestBuilder = Glide.with(LocalView.current)
                .asBitmap()
                .placeholder(logo.logoId)
                .error(logo.logoId)
                .fallback(logo.logoId),
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    ColorUtils
                        .setAlphaComponent(
                            ColorUtils.blendARGB(
                                MaterialTheme.colors.primarySurface.toArgb(),
                                swatchInfo?.rgb ?: Color.Transparent.toArgb(),
                                0.25f
                            ),
                            200//127
                        )
                        .toComposeColor()
                )
        )

        Row(
            modifier = Modifier
                .padding(5.dp)
                .animateContentSize()
        ) {

            Card(
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.padding(5.dp)
            ) {
                GlideImage(
                    imageModel = model.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    requestBuilder = Glide.with(LocalView.current)
                        .asBitmap()
                        //.override(360, 480)
                        .placeholder(logo.logoId)
                        .error(logo.logoId)
                        .fallback(logo.logoId)
                        .transform(RoundedCorners(5)),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    failure = {
                        Image(
                            painter = painterResource(id = logo.logoId),
                            contentDescription = model.title,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    }
                )
            }

            Column(
                modifier = Modifier.padding(start = 5.dp)
            ) {

                LazyRow(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(model.genres) {
                        CustomChip(
                            category = it,
                            textColor = swatchInfo?.rgb?.toComposeColor(),
                            backgroundColor = swatchInfo?.bodyColor?.toComposeColor()?.copy(1f),
                            modifier = Modifier.fadeInAnimation()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clickable { favoriteClick(isFavorite) }
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                ) {

                    /*val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.heart))
                    val p by animateLottieCompositionAsState(composition = composition)

                    *//*
                        fun LottieAnimationView.changeTint(@ColorInt newColor: Int) =
        addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_ATOP) }
                         *//*

                        LottieAnimation(
                            composition = composition,
                            progress = animateFloatAsState(targetValue = ),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )*/

                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = swatchInfo?.rgb?.toComposeColor() ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        stringResource(if (isFavorite) R.string.removeFromFavorites else R.string.addToFavorites),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Text(
                    model.description,
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .clickable { descriptionVisibility = !descriptionVisibility },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                    style = MaterialTheme.typography.body2,
                )

            }

        }
    }
}