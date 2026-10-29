package com.programmersbox.kmpuiviews.presentation.details


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
import androidx.compose.material.icons.filled.BrokenImage
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.utils.ComposableUtils
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.composables.modifiers.fadeInAnimation
import com.programmersbox.kmpuiviews.utils.composables.modifiers.scaleRotateOffsetReset
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuImageElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.SharedItemKey
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.OtakuTitleElement
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.customSharedElement
import com.programmersbox.kmpuiviews.zoomOverlay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.addToFavorites
import otakuworld.kmpuiviews.generated.resources.chapter_count
import otakuworld.kmpuiviews.generated.resources.done
import otakuworld.kmpuiviews.generated.resources.removeFromFavorites

private val BannerHeight = 180.dp
private val CoverOverlap = 70.dp

/** The item that opened Details; keys its cover and title for the shared element transition */
internal val LocalDetailsSharedItemKey = staticCompositionLocalOf<SharedItemKey?> { null }

/** The cover the tapped card handed off, already decoded */
internal val LocalDetailsHandedOffCover = staticCompositionLocalOf<ImageBitmap?> { null }

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
    val scope = rememberCoroutineScope()
    val sharedItemKey = LocalDetailsSharedItemKey.current

    val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
    val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

    val imageUrl = model.imageUrl

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
                    //placeHolder = { painterLogo() },
                    placeHolder = { rememberVectorPainter(Icons.Default.BrokenImage) },
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter,
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
        // Fixed-height blurred banner — BannerHeight gives a cinematic anchor
        ImageLoaderChoice(
            imageUrl = imageUrl,
            name = "",
            headers = model.extras.mapValues { it.value.toString() },
            placeHolder = { blurHash ?: rememberVectorPainter(Icons.Default.BrokenImage) },
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = Modifier
                .matchParentSize()
                .composed {
                    val brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                    this
                        .blur(8.dp)
                        .drawWithContent {
                            drawContent()
                            drawRect(brush)
                        }
                }
        )

        // Content column — padding(top = BannerHeight - CoverOverlap)
        // This makes the cover art visually "float" over the banner's lower edge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .animateContentSize()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(4.dp)
                        .customSharedElement(sharedItemKey?.let(::OtakuImageElement))
                        .zoomOverlay()
                ) {
                    // The loader fades even a cached image in from transparent, so the handed-off cover is drawn
                    // directly; the palette was already built from it
                    val handedOffCover = LocalDetailsHandedOffCover.current
                    if (handedOffCover != null) {
                        Image(
                            bitmap = handedOffCover,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            colorFilter = colorFilter,
                            modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                        )
                    } else {
                        ImageLoaderChoice(
                            imageUrl = imageUrl,
                            name = "",
                            headers = model.extras.mapValues { it.value.toString() },
                            contentScale = ContentScale.FillBounds,
                            placeHolder = { blurHash ?: rememberVectorPainter(Icons.Default.BrokenImage) },
                            error = { rememberVectorPainter(Icons.Default.BrokenImage) },
                            onImageSet = onBitmapSet,
                            colorFilter = colorFilter,
                            modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .animateContentSize()
                ) {
                    Text(
                        model.source.displayName,
                        style = MaterialTheme.typography.labelSmall,
                    )

                    var descriptionVisibility by remember { mutableStateOf(false) }
                    val clipboard = LocalClipboardManager.current
                    Text(
                        model.title,
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .customSharedElement(sharedItemKey?.let(::OtakuTitleElement))
                            .combinedClickable(
                                interactionSource = null,
                                indication = ripple(),
                                onClick = { descriptionVisibility = !descriptionVisibility },
                                onLongClick = {
                                    scope.launch {
                                        clipboard.setText(
                                            buildAnnotatedString { append(model.title) }
                                        )
                                    }
                                }
                            )
                            .fillMaxWidth(),
                    )

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
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
    imageUrl: String? = null,
    title: String? = null,
    coverBitmap: ImageBitmap? = null,
) {
    val sharedItemKey = LocalDetailsSharedItemKey.current
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
                        .then(if (bitmapPainter != null || imageUrl != null) Modifier else placeholderModifier)
                        .padding(4.dp)
                        .customSharedElement(sharedItemKey?.takeIf { imageUrl != null }?.let(::OtakuImageElement))
                ) {
                    // The card's own decoded cover, when it handed one off; no second load needed
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    } else if (imageUrl != null) {
                        ImageLoaderChoice(
                            imageUrl = imageUrl,
                            name = "",
                            placeHolder = { bitmapPainter ?: rememberVectorPainter(Icons.Default.BrokenImage) },
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    } else if (bitmapPainter != null) {
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
                    if (title != null) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .customSharedElement(sharedItemKey?.let(::OtakuTitleElement))
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .then(placeholderModifier)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) { Text("") }
                    }

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