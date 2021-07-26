package com.programmersbox.uiviews.utils

import android.content.res.ColorStateList
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.programmersbox.helpfulutils.changeDrawableColor
import com.programmersbox.helpfulutils.whatIfNotNull
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.R
import com.skydoves.landscapist.glide.GlideImage
import kotlin.math.ceil

@BindingAdapter("coverImage", "logoId")
fun loadImage(view: ImageView, imageUrl: String?, logoId: Int) {
    Glide.with(view)
        .load(imageUrl)
        .override(360, 480)
        .placeholder(logoId)
        .error(logoId)
        .fallback(logoId)
        .transform(RoundedCorners(15))
        .into(view)
}

@BindingAdapter("otherNames")
fun otherNames(view: TextView, names: List<String>?) {
    view.text = names?.joinToString("\n\n")
}

@BindingAdapter("genreList", "swatch")
fun loadGenres(view: ChipGroup, genres: List<String>?, swatchInfo: SwatchInfo?) {
    view.removeAllViews()
    genres?.forEach {
        view.addView(Chip(view.context).apply {
            text = it
            isCheckable = false
            isClickable = false
            swatchInfo?.rgb?.let { setTextColor(it) }
            swatchInfo?.bodyColor?.let { chipBackgroundColor = ColorStateList.valueOf(it) }
        })
    }
}

@BindingAdapter("toolbarColors")
fun toolbarColors(view: Toolbar, swatchInfo: SwatchInfo?) {
    swatchInfo?.titleColor?.let {
        view.setTitleTextColor(it)
        view.navigationIcon?.changeDrawableColor(it)
        view.setSubtitleTextColor(it)
        view.overflowIcon?.changeDrawableColor(it)
    }
    swatchInfo?.rgb?.let { view.setBackgroundColor(it) }
}

@BindingAdapter("collapsingToolbarColors")
fun collapsingToolbarColors(view: CollapsingToolbarLayout, swatchInfo: SwatchInfo?) {
    swatchInfo?.titleColor?.let { view.setCollapsedTitleTextColor(it) }
    swatchInfo?.rgb?.let {
        view.setBackgroundColor(it)
        //view.setExpandedTitleColor(it)
    }
}

@BindingAdapter("titleColor")
fun titleColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("bodyColor")
fun bodyColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("linkColor")
fun linkColor(view: TextView, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.setLinkTextColor(it) }
}

@BindingAdapter("optionTint")
fun optionTint(view: MaterialButton, swatchInfo: SwatchInfo?) {
    swatchInfo?.rgb?.let { view.strokeColor = ColorStateList.valueOf(it) }
}

@BindingAdapter("checkedButtonTint")
fun buttonTint(view: CheckBox, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.buttonTintList = ColorStateList.valueOf(it) }
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
}

@BindingAdapter("startButtonColor")
fun startButtonColor(view: MaterialButton, swatchInfo: SwatchInfo?) {
    swatchInfo?.bodyColor?.let { view.iconTint = ColorStateList.valueOf(it) }
    swatchInfo?.bodyColor?.let { view.setTextColor(it) }
    swatchInfo?.bodyColor?.let { view.strokeColor = ColorStateList.valueOf(it) }
}

@BindingAdapter("uploadedText")
fun uploadedText(view: TextView, chapterModel: ChapterModel) {
    /*if (
        chapterModel.uploadedTime != null &&
        chapterModel.uploadedTime?.isDateBetween(System.currentTimeMillis() - 8.days.inMilliseconds.toLong(), System.currentTimeMillis()) == true
    ) {
        view.setTimeAgo(chapterModel.uploadedTime!!, showSeconds = true, autoUpdate = false)
    } else {

    }*/
    view.text = chapterModel.uploaded
}

object ComposableUtils {

    val IMAGE_WIDTH @Composable get() = with(LocalDensity.current) { 360.toDp() }
    val IMAGE_HEIGHT @Composable get() = with(LocalDensity.current) { 480.toDp() }

}

@Composable
fun StaggeredVerticalGrid(
    modifier: Modifier = Modifier,
    maxColumnWidth: Dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeableXY: MutableMap<Placeable, Pair<Int, Int>> = mutableMapOf()

        check(constraints.hasBoundedWidth) {
            "Unbounded width not supported"
        }
        val columns = ceil(constraints.maxWidth / maxColumnWidth.toPx()).toInt()
        val columnWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(maxWidth = columnWidth)
        val colHeights = IntArray(columns) { 0 } // track each column's height
        val placeables = measurables.map { measurable ->
            val column = shortestColumn(colHeights)
            val placeable = measurable.measure(itemConstraints)
            placeableXY[placeable] = Pair(columnWidth * column, colHeights[column])
            colHeights[column] += placeable.height
            placeable
        }

        val height = colHeights.maxOrNull()
            ?.coerceIn(constraints.minHeight, constraints.maxHeight)
            ?: constraints.minHeight
        layout(
            width = constraints.maxWidth,
            height = height
        ) {
            placeables.forEach { placeable ->
                placeable.place(
                    x = placeableXY.getValue(placeable).first,
                    y = placeableXY.getValue(placeable).second
                )
            }
        }
    }
}

@Composable
fun StaggeredVerticalGrid(
    modifier: Modifier = Modifier,
    columns: Int,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeableXY: MutableMap<Placeable, Pair<Int, Int>> = mutableMapOf()

        check(constraints.hasBoundedWidth) {
            "Unbounded width not supported"
        }
        val columnWidth = constraints.maxWidth / columns
        val itemConstraints = constraints.copy(maxWidth = columnWidth)
        val colHeights = IntArray(columns) { 0 } // track each column's height
        val placeables = measurables.map { measurable ->
            val column = shortestColumn(colHeights)
            val placeable = measurable.measure(itemConstraints)
            placeableXY[placeable] = Pair(columnWidth * column, colHeights[column])
            colHeights[column] += placeable.height
            placeable
        }

        val height = colHeights.maxOrNull()
            ?.coerceIn(constraints.minHeight, constraints.maxHeight)
            ?: constraints.minHeight
        layout(
            width = constraints.maxWidth,
            height = height
        ) {
            placeables.forEach { placeable ->
                placeable.place(
                    x = placeableXY.getValue(placeable).first,
                    y = placeableXY.getValue(placeable).second
                )
            }
        }
    }
}

private fun shortestColumn(colHeights: IntArray): Int {
    var minHeight = Int.MAX_VALUE
    var column = 0
    colHeights.forEachIndexed { index, height ->
        if (height < minHeight) {
            minHeight = height
            column = index
        }
    }
    return column
}

@Composable
fun DetailsHeader(
    model: InfoModel,
    logo: MainLogo,
    swatchInfo: SwatchInfo?,
    isFavorite: Boolean,
    favoriteClick: (Boolean) -> Unit
) {

    var descriptionVisibility by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
            .whatIfNotNull(swatchInfo?.rgb) {
                background(
                    ColorUtils.setAlphaComponent(
                        ColorUtils.blendARGB(
                            MaterialTheme.colors.surface.toArgb(),
                            it,
                            0.25f
                        ),
                        127
                    ).toComposeColor()
                )
            }
    ) {

        Row(
            modifier = Modifier
                .padding(vertical = 5.dp)
                .padding(end = 5.dp)
                .animateContentSize()
                .whatIfNotNull(swatchInfo?.rgb) {
                    background(
                        ColorUtils.setAlphaComponent(
                            ColorUtils.blendARGB(
                                MaterialTheme.colors.surface.toArgb(),
                                it,
                                0.25f
                            ),
                            127
                        ).toComposeColor()
                    )
                }
        ) {

            Card(
                modifier = Modifier.padding(start = 5.dp)
            ) {
                GlideImage(
                    imageModel = model.imageUrl,
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    requestBuilder = Glide.with(LocalView.current)
                        .asBitmap()
                        .override(360, 480)
                        .placeholder(logo.logoId)
                        .error(logo.logoId)
                        .fallback(logo.logoId)
                        .transform(RoundedCorners(15)),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        //.padding(5.dp)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    failure = {
                        Image(
                            painter = painterResource(logo.logoId),
                            contentDescription = model.title,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                //.padding(5.dp)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    }
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 5.dp)
            ) {

                LazyRow(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(model.genres) {
                        CustomChip(
                            category = it,
                            textColor = swatchInfo?.rgb?.toComposeColor(),
                            backgroundColor = swatchInfo?.bodyColor?.toComposeColor()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clickable { favoriteClick(isFavorite) }
                        .padding(vertical = 5.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "",
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
                        .weight(1f)
                        .clickable { descriptionVisibility = !descriptionVisibility },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                    style = MaterialTheme.typography.body2,
                )

            }

        }
    }
}

fun Int.toComposeColor() = Color(this)

@Composable
fun CustomChip(
    category: String,
    textColor: Color?,
    backgroundColor: Color?
) {
    Surface(
        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor ?: MaterialTheme.colors.surface
    ) {
        Row {
            Text(
                text = category,
                style = MaterialTheme.typography.body2,
                color = textColor ?: MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

/*
object TestItems {

    val TEST_SOURCE = object : ApiService {
        override val baseUrl: String get() = ""
        override fun getRecent(page: Int): Single<List<ItemModel>> = Single.never()
        override fun getList(page: Int): Single<List<ItemModel>> = Single.never()
        override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.never()
        override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.never()
    }

    val TEST_SWATCH = SwatchInfo(
        rgb = Color.Blue.toArgb(),
        titleColor = Color.Red.toArgb(),
        bodyColor = Color.Green.toArgb()
    )
}
*/
