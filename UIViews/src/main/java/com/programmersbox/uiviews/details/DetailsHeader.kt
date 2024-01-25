package com.programmersbox.uiviews.details

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.ripple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.load.model.GlideUrl
import com.programmersbox.models.InfoModel
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.animate
import com.programmersbox.uiviews.utils.components.placeholder.PlaceholderHighlight
import com.programmersbox.uiviews.utils.components.placeholder.m3placeholder
import com.programmersbox.uiviews.utils.components.placeholder.shimmer
import com.programmersbox.uiviews.utils.fadeInAnimation
import com.programmersbox.uiviews.utils.scaleRotateOffsetReset
import com.programmersbox.uiviews.utils.toComposeColor
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
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
) {
    val swatchChange = LocalSwatchChange.current
    val swatchInfo = LocalSwatchInfo.current.colors
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
                    previewPlaceholder = R.drawable.ic_baseline_battery_alert_24,
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
                    swatchInfo?.rgb ?: Color.Transparent.toArgb(),
                    0.25f
                ),
                200
            )
            .toComposeColor()
            .animate()

        GlideImage(
            imageModel = { imageUrl },
            imageOptions = ImageOptions(contentScale = ContentScale.Crop),
            previewPlaceholder = R.drawable.ic_baseline_battery_alert_24,
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
                    modifier = Modifier.padding(4.dp)
                ) {
                    val latestSwatch by rememberUpdatedState(newValue = swatchInfo)
                    GlideImage(
                        imageModel = { imageUrl },
                        imageOptions = ImageOptions(contentScale = ContentScale.FillBounds),
                        component = rememberImageComponent {
                            +PalettePlugin { p ->
                                if (latestSwatch == null) {
                                    p.vibrantSwatch
                                        ?.let { s -> SwatchInfo(s.rgb, s.titleTextColor, s.bodyTextColor) }
                                        ?.let(swatchChange)
                                }
                            }
                            +PlaceholderPlugin.Loading(logo)
                            +PlaceholderPlugin.Failure(logo)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .combinedClickable(
                                onClick = {},
                                onDoubleClick = { imagePopup = true }
                            )
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
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    var descriptionVisibility by remember { mutableStateOf(false) }

                    Text(
                        model.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable(
                                interactionSource = null,
                                indication = ripple()
                            ) { descriptionVisibility = !descriptionVisibility }
                            .fillMaxWidth(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                        color = MaterialTheme.colorScheme.onSurface
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
                            tint = swatchInfo?.rgb?.toComposeColor()?.animate()?.value
                                ?: MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Crossfade(targetState = isFavorite, label = "") { target ->
                            Text(
                                stringResource(if (target) R.string.removeFromFavorites else R.string.addToFavorites),
                                style = MaterialTheme.typography.headlineSmall,
                                fontSize = 20.sp,
                                modifier = Modifier.align(Alignment.CenterVertically),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        stringResource(R.string.chapter_count, model.chapters.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
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
                //mainAxisSpacing = 4.dp,
                //crossAxisSpacing = 2.dp,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                model.genres.forEach {
                    AssistChip(
                        onClick = {},
                        modifier = Modifier.fadeInAnimation(),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = (swatchInfo?.rgb?.toComposeColor() ?: MaterialTheme.colorScheme.onSurface)
                                .animate().value,
                            labelColor = (swatchInfo?.bodyColor?.toComposeColor()?.copy(1f) ?: MaterialTheme.colorScheme.surface)
                                .animate().value
                        ),
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
internal fun PlaceHolderHeader(paddingValues: PaddingValues) {
    val placeholderModifier = Modifier.m3placeholder(
        true,
        highlight = PlaceholderHighlight.shimmer()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        Row(modifier = Modifier.padding(4.dp)) {

            Card(
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .then(placeholderModifier)
                    .padding(4.dp)
            ) {
                Image(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)

                )
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