package com.programmersbox.uiviews.details

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.load.model.GlideUrl
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.animate
import com.programmersbox.uiviews.utils.components.placeholder.PlaceholderHighlight
import com.programmersbox.uiviews.utils.components.placeholder.m3placeholder
import com.programmersbox.uiviews.utils.components.placeholder.shimmer
import com.programmersbox.uiviews.utils.fadeInAnimation
import com.programmersbox.uiviews.utils.scaleRotateOffsetReset
import com.programmersbox.uiviews.utils.sharedelements.OtakuImageElement
import com.programmersbox.uiviews.utils.sharedelements.OtakuTitleElement
import com.programmersbox.uiviews.utils.sharedelements.customSharedElement
import com.programmersbox.uiviews.utils.toComposeColor
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.glide.GlideImageState
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.placeholder.placeholder.PlaceholderPlugin

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
internal fun DetailsHeader(
    model: InfoModel,
    logo: Any?,
    isFavorite: Boolean,
    favoriteClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    possibleDescription: @Composable () -> Unit = {},
    onPaletteSet: (Palette) -> Unit,
    blurHash: BitmapPainter? = null,
    onBitmapSet: (Bitmap) -> Unit = {},
) {
    val surface = MaterialTheme.colorScheme.surface
    val imageUrl = remember {
        try {
            GlideUrl(model.imageUrl) { model.extras.map { it.key to it.value.toString() }.toMap() }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            val b = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
            Canvas(b).drawColor(surface.toArgb())
            b
        }
    }

    var imagePopup by remember { mutableStateOf(false) }

    if (imagePopup) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { imagePopup = false },
            title = { Text(model.title, modifier = Modifier.padding(4.dp)) },
            text = {
                GlideImage(
                    imageModel = { imageUrl },
                    imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                    previewPlaceholder = painterResource(id = R.drawable.ic_baseline_battery_alert_24),
                    modifier = Modifier
                        .scaleRotateOffsetReset()
                        .defaultMinSize(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                )
            },
            confirmButton = { TextButton(onClick = { imagePopup = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        val color by ColorUtils
            .setAlphaComponent(
                ColorUtils.blendARGB(
                    MaterialTheme.colorScheme.surface.toArgb(),
                    MaterialTheme.colorScheme.primary.toArgb(),
                    0.25f
                ),
                200
            )
            .toComposeColor()
            .animate()

        GlideImage(
            imageModel = { imageUrl },
            imageOptions = ImageOptions(contentScale = ContentScale.Crop),
            previewPlaceholder = painterResource(id = R.drawable.ic_baseline_battery_alert_24),
            component = rememberImageComponent {
                +PlaceholderPlugin.Loading(blurHash ?: logo)
            },
            modifier = Modifier
                .matchParentSize()
                .blur(4.dp)
                .drawWithContent {
                    drawContent()
                    drawRect(color)
                },
        )

        Column(
            modifier = Modifier
                .padding(4.dp)
                .animateContentSize()
        ) {
            Row {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .padding(4.dp)
                        .customSharedElement(
                            OtakuImageElement(
                                origin = model.imageUrl,
                                source = model.title,
                            )
                        )
                ) {
                    var magnifierCenter by remember { mutableStateOf(Offset.Unspecified) }

                    GlideImage(
                        imageModel = { imageUrl },
                        imageOptions = ImageOptions(contentScale = ContentScale.FillBounds),
                        component = rememberImageComponent {
                            +PalettePlugin { p -> onPaletteSet(p) }
                            +PlaceholderPlugin.Loading(blurHash ?: logo)
                            +PlaceholderPlugin.Failure(logo)
                        },
                        onImageStateChanged = {
                            if (it is GlideImageState.Success) {
                                it.imageBitmap?.asAndroidBitmap()?.let { it1 -> onBitmapSet(it1) }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            /*.combinedClickable(
                                onClick = {},
                                onDoubleClick = { imagePopup = true }
                            )*/
                            .magnifier(
                                sourceCenter = { magnifierCenter },
                                magnifierCenter = { magnifierCenter.copy(y = magnifierCenter.y - 100) },
                                zoom = 3f,
                                size = DpSize(100.dp, 100.dp),
                                cornerRadius = 8.dp
                            )
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    // Show the magnifier at the original pointer position.
                                    onDragStart = { magnifierCenter = it },
                                    // Make the magnifier follow the finger while dragging.
                                    onDrag = { _, delta -> magnifierCenter += delta },
                                    // Hide the magnifier when the finger lifts.
                                    onDragEnd = { magnifierCenter = Offset.Unspecified },
                                    onDragCancel = { magnifierCenter = Offset.Unspecified }
                                )
                            }
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        model.source.serviceName,
                        style = MaterialTheme.typography.labelSmall,
                    )

                    var descriptionVisibility by remember { mutableStateOf(false) }

                    Text(
                        model.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .customSharedElement(
                                OtakuTitleElement(
                                    origin = model.title,
                                    source = model.title
                                )
                            )
                            .clickable(
                                interactionSource = null,
                                indication = ripple()
                            ) { descriptionVisibility = !descriptionVisibility }
                            .fillMaxWidth(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                    )

                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = null,
                                indication = ripple()
                            ) { favoriteClick(isFavorite) }
                            .semantics(true) {}
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Crossfade(targetState = isFavorite, label = "") { target ->
                            Text(
                                stringResource(if (target) R.string.removeFromFavorites else R.string.addToFavorites),
                                style = MaterialTheme.typography.headlineSmall,
                                fontSize = 20.sp,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }

                    Text(
                        stringResource(R.string.chapter_count, model.chapters.size),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    /*if(model.alternativeNames.isNotEmpty()) {
                        Text(
                            stringResource(R.string.alternateNames, model.alternativeNames.joinToString(", ")),
                            maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { descriptionVisibility = !descriptionVisibility }
                        )
                    }*/

                    /*
                    var descriptionVisibility by remember { mutableStateOf(false) }
                    Text(
                        model.description,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { descriptionVisibility = !descriptionVisibility },
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                        style = MaterialTheme.typography.body2,
                    )*/
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                model.genres.forEach {
                    AssistChip(
                        onClick = {},
                        modifier = Modifier.fadeInAnimation(),
                        label = { Text(it) }
                    )
                }
            }
            possibleDescription()
        }
    }
}


@ExperimentalFoundationApi
@Composable
internal fun PlaceHolderHeader(
    paddingValues: PaddingValues,
    bitmapPainter: BitmapPainter? = null,
) {
    val placeholderModifier = Modifier.m3placeholder(
        true,
        highlight = PlaceholderHighlight.shimmer()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (bitmapPainter != null) {
                Image(
                    painter = bitmapPainter,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                )
            }

            Row(modifier = Modifier.padding(4.dp)) {

                Card(
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .then(if (bitmapPainter != null) Modifier else placeholderModifier)
                        .padding(4.dp)
                ) {
                    if (bitmapPainter != null) {
                        Image(
                            painter = bitmapPainter,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    } else {
                        Image(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .then(placeholderModifier)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) { Text("") }

                    Row(
                        modifier = Modifier
                            .then(placeholderModifier)
                            .semantics(true) {}
                            .padding(vertical = 4.dp)
                            .fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            stringResource(R.string.addToFavorites),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Text(
                        "Otaku".repeat(50),
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth()
                            .then(placeholderModifier),
                        maxLines = 2
                    )
                }
            }
        }
    }
}