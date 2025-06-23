package com.programmersbox.kmpuiviews.presentation.details


import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.composables.modifiers.fadeInAnimation
import com.programmersbox.kmpuiviews.utils.composables.modifiers.scaleRotateOffsetReset
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuImageElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuTitleElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.customSharedElement
import com.programmersbox.kmpuiviews.zoomOverlay
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.addToFavorites
import otakuworld.kmpuiviews.generated.resources.chapter_count
import otakuworld.kmpuiviews.generated.resources.done
import otakuworld.kmpuiviews.generated.resources.removeFromFavorites

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
internal fun DetailsHeader(
    model: KmpInfoModel,
    isFavorite: Boolean,
    favoriteClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    possibleDescription: @Composable () -> Unit = {},
    onPaletteSet: (Palette) -> Unit,
    blurHash: BitmapPainter? = null,
    onBitmapSet: (ImageBitmap) -> Unit = {},
) {
    val settings = LocalSettingsHandling.current

    val blurEnabled by settings.rememberShowBlur()
    val surface = MaterialTheme.colorScheme.surface
    val imageUrl = model.imageUrl/*remember {
        try {
            GlideUrl(model.imageUrl) { model.extras.mapValues { it.value.toString() } }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            val b = createBitmap(5, 5)
            Canvas(b).drawColor(surface.toArgb())
            b
        }
    }*/

    var imagePopup by remember { mutableStateOf(false) }

    if (imagePopup) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { imagePopup = false },
            title = { Text(model.title, modifier = Modifier.padding(4.dp)) },
            text = {
                ImageLoaderChoice(
                    imageUrl = imageUrl,
                    name = "",
                    headers = model.extras.mapValues { it.value.toString() },
                    placeHolder = { painterLogo() },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .scaleRotateOffsetReset()
                        .defaultMinSize(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                )
            },
            confirmButton = { TextButton(onClick = { imagePopup = false }) { Text(stringResource(Res.string.done)) } }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {

        val haze = rememberHazeState()

        ImageLoaderChoice(
            imageUrl = imageUrl,
            name = "",
            headers = model.extras.mapValues { it.value.toString() },
            placeHolder = { painterLogo() },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .let {
                    if (blurEnabled) {
                        it.hazeSource(haze)
                    } else {
                        it.blur(4.dp)
                    }
                }
        )

        Column(
            modifier = Modifier
                .hazeEffect(
                    haze,
                    style = HazeStyle(
                        blurRadius = 12.dp,
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        tint = HazeTint(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = if (MaterialTheme.colorScheme.surface.luminance() >= 0.5) 0.35f else 0.55f
                            ),
                        )
                    )
                )
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
                        .zoomOverlay()
                ) {
                    //var magnifierCenter by remember { mutableStateOf(Offset.Unspecified) }

                    ImageLoaderChoice(
                        imageUrl = imageUrl,
                        name = "",
                        headers = model.extras.mapValues { it.value.toString() },
                        contentScale = ContentScale.FillBounds,
                        placeHolder = { painterLogo() },
                        onImageSet = onBitmapSet,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            /*.combinedClickable(
                                onClick = {},
                                onDoubleClick = { imagePopup = true }
                            )*/
                            /*.magnifier(
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
                            }*/
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
                    SelectionContainer {
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
                    }
                    Crossfade(targetState = isFavorite, label = "") { target ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable(
                                    interactionSource = null,
                                    indication = ripple()
                                ) { favoriteClick(isFavorite) }
                                .padding(4.dp)
                                .semantics(true) {}
                                .fillMaxWidth()
                        ) {
                            Icon(
                                if (target) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Text(
                                stringResource(if (target) Res.string.removeFromFavorites else Res.string.addToFavorites),
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 16.sp,
                            )
                        }
                    }

                    Text(
                        stringResource(Res.string.chapter_count, model.chapters.size),
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
                            stringResource(Res.string.addToFavorites),
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