package com.programmersbox.uiviews.presentation.components

import android.util.LruCache
import androidx.compose.material.icons.filled.Palette
import coil.Coil

/*@Composable
fun rememberDominantColorState(
    context: Context = LocalContext.current,
    defaultColor: Color = MaterialTheme.colorScheme.primary,
    defaultOnColor: Color = MaterialTheme.colorScheme.onPrimary,
    cacheSize: Int = 12,
    isColorValid: (Color) -> Boolean = { true }
): DominantColorState = remember {
    DominantColorState(context, defaultColor, defaultOnColor, cacheSize, isColorValid)
}

@Composable
fun rememberDynamicColorState(
    context: Context = LocalContext.current,
    defaultColorScheme: ColorScheme = MaterialTheme.colorScheme,
    cacheSize: Int = 12
): DynamicColorState = remember {
    DynamicColorState(context, defaultColorScheme, cacheSize)
}*/

//TODO: Try converting DetailsFragment to use this

/*
val surfaceColor = M3MaterialTheme.colorScheme.surface
val dominantColor = rememberDominantColorState { color ->
    // We want a color which has sufficient contrast against the surface color
    color.contrastAgainst(surfaceColor) >= 3f
}

DynamicThemePrimaryColorsFromImage(dominantColor) {
    LaunchedEffect(item.imageUrl) {
        if (item.imageUrl != null) {
            dominantColor.updateColorsFromImageUrl(item.imageUrl)
        } else {
            dominantColor.reset()
        }
    }
    HistoryItem(item, scope)
}
 */

/**
 * A composable which allows dynamic theming of the [androidx.compose.material.Colors.primary]
 * color from an image.
 *//*
@Composable
fun DynamicThemePrimaryColorsFromImage(
    dominantColorState: DominantColorState = rememberDominantColorState(),
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme.copy(
        primary = animateColorAsState(
            dominantColorState.color,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onPrimary = animateColorAsState(
            dominantColorState.onColor,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        surface = animateColorAsState(
            dominantColorState.color,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onSurface = animateColorAsState(
            dominantColorState.onColor,
            spring(stiffness = Spring.StiffnessLow)
        ).value
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun FullDynamicThemePrimaryColorsFromImage(
    dominantColorState: DynamicColorState = rememberDynamicColorState(),
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme.copy(
        primary = animateColorAsState(
            dominantColorState.colorScheme.primary,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onPrimary = animateColorAsState(
            dominantColorState.colorScheme.onPrimary,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        surface = animateColorAsState(
            dominantColorState.colorScheme.surface,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onSurface = animateColorAsState(
            dominantColorState.colorScheme.onSurface,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        background = animateColorAsState(
            dominantColorState.colorScheme.background,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
        onBackground = animateColorAsState(
            dominantColorState.colorScheme.onBackground,
            spring(stiffness = Spring.StiffnessLow)
        ).value,
    )
    MaterialTheme(colorScheme = colors, content = content)
}*/

/**
 * A class which stores and caches the result of any calculated dominant colors
 * from images.
 *
 * @param context Android context
 * @param defaultColor The default color, which will be used if [calculateDominantColor] fails to
 * calculate a dominant color
 * @param defaultOnColor The default foreground 'on color' for [defaultColor].
 * @param cacheSize The size of the [LruCache] used to store recent results. Pass `0` to
 * disable the cache.
 * @param isColorValid A lambda which allows filtering of the calculated image colors.
 *//*
@Stable
class DominantColorState(
    private val context: Context,
    private val defaultColor: Color,
    private val defaultOnColor: Color,
    cacheSize: Int = 12,
    private val isColorValid: (Color) -> Boolean = { true }
) {
    var color by mutableStateOf(defaultColor)
        private set
    var onColor by mutableStateOf(defaultOnColor)
        private set

    private val cache = when {
        cacheSize > 0 -> LruCache<String, DominantColors>(cacheSize)
        else -> null
    }

    suspend fun updateColorsFromImageUrl(url: String) {
        val result = calculateDominantColor(url)
        color = result?.color ?: defaultColor
        onColor = result?.onColor ?: defaultOnColor
    }

    private suspend fun calculateDominantColor(url: String): DominantColors? {
        val cached = cache?.get(url)
        if (cached != null) {
            // If we already have the result cached, return early now...
            return cached
        }

        // Otherwise we calculate the swatches in the image, and return the first valid color
        return calculateSwatchesInImage(context, url)
            // First we want to sort the list by the color's population
            .sortedByDescending { swatch -> swatch.population }
            // Then we want to find the first valid color
            .firstOrNull { swatch -> isColorValid(Color(swatch.rgb)) }
            // If we found a valid swatch, wrap it in a [DominantColors]
            ?.let { swatch ->
                DominantColors(
                    color = Color(swatch.rgb),
                    onColor = Color(swatch.bodyTextColor).copy(alpha = 1f)
                )
            }
            // Cache the resulting [DominantColors]
            ?.also { result -> cache?.put(url, result) }
    }

    */
/**
     * Reset the color values to [defaultColor].
 *//*
    fun reset() {
        color = defaultColor
        onColor = defaultColor
    }
}

@Stable
class DynamicColorState(
    private val context: Context,
    private val defaultColorScheme: ColorScheme,
    cacheSize: Int = 12
) {
    var colorScheme by mutableStateOf(defaultColorScheme)
        private set

    private val cache = when {
        cacheSize > 0 -> LruCache<String, Palette>(cacheSize)
        else -> null
    }

    suspend fun updateColorsFromImageUrl(url: String) {
        val result = calculateDominantColor(url)
        colorScheme = result?.let {
            defaultColorScheme.copy(
                primary = it.dominantSwatch?.rgb?.let { it1 -> Color(it1) } ?: defaultColorScheme.primary,
                onPrimary = it.dominantSwatch?.let { it1 -> Color(it1.bodyTextColor) } ?: defaultColorScheme.onPrimary,
                surface = it.vibrantSwatch?.let { it1 -> Color(it1.rgb) } ?: defaultColorScheme.surface,
                onSurface = it.vibrantSwatch?.let { it1 -> Color(it1.bodyTextColor) } ?: defaultColorScheme.onSurface,
                background = it.mutedSwatch?.let { it1 -> Color(it1.rgb) } ?: defaultColorScheme.background,
                onBackground = it.mutedSwatch?.let { it1 -> Color(it1.bodyTextColor) } ?: defaultColorScheme.onBackground,
            )
        } ?: defaultColorScheme
    }

    private suspend fun calculateDominantColor(url: String): Palette? {
        val cached = cache?.get(url)
        if (cached != null) {
            // If we already have the result cached, return early now...
            return cached
        }

        // Otherwise we calculate the swatches in the image, and return the first valid color
        return calculateAllSwatchesInImage(context, url)
            // First we want to sort the list by the color's population
            //.sortedByDescending { swatch -> swatch.population }
            // Then we want to find the first valid color
            //.firstOrNull { swatch -> isColorValid(Color(swatch.rgb)) }
            // If we found a valid swatch, wrap it in a [DominantColors]
            *//*?.let { swatch ->
                DominantColors(
                    color = Color(swatch.rgb),
                    onColor = Color(swatch.bodyTextColor).copy(alpha = 1f)
                )
            }*//*
            // Cache the resulting [DominantColors]
            ?.also { result -> cache?.put(url, result) }
    }

    */
/**
     * Reset the color values to [defaultColor].
 *//*
    fun reset() {
        colorScheme = defaultColorScheme
    }
}

@Immutable
private data class DominantColors(val color: Color, val onColor: Color)

*/
/**
 * Fetches the given [imageUrl] with [Coil], then uses [Palette] to calculate the dominant color.
 *//*
private suspend fun calculateSwatchesInImage(
    context: Context,
    imageUrl: String
): List<Palette.Swatch> {
    val r = ImageRequest.Builder(context)
        .data(imageUrl)
        // We scale the image to cover 128px x 128px (i.e. min dimension == 128px)
        .size(128).scale(Scale.FILL)
        // Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
        .allowHardware(false)
        .build()

    val bitmap = when (val result = r.context.imageLoader.execute(r)) {
        is SuccessResult -> result.drawable.toBitmap()
        else -> null
    }

    return bitmap?.let {
        withContext(Dispatchers.Default) {
            val palette = Palette.Builder(bitmap)
                // Disable any bitmap resizing in Palette. We've already loaded an appropriately
                // sized bitmap through Coil
                .resizeBitmapArea(0)
                // Clear any built-in filters. We want the unfiltered dominant color
                .clearFilters()
                // We reduce the maximum color count down to 8
                .maximumColorCount(8)
                .generate()
            //TODO: Maybe change this to return the palette and choose the kind of swatch afterwards
            // dominant
            // vibrant
            // muted
            palette.vibrantSwatch?.let { listOfNotNull(it) } ?: palette.swatches
        }
    } ?: emptyList()
}

private suspend fun calculateAllSwatchesInImage(
    context: Context,
    imageUrl: String
): Palette? {
    val r = ImageRequest.Builder(context)
        .data(imageUrl)
        // We scale the image to cover 128px x 128px (i.e. min dimension == 128px)
        .size(128).scale(Scale.FILL)
        // Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
        .allowHardware(false)
        .build()

    val bitmap = when (val result = r.context.imageLoader.execute(r)) {
        is SuccessResult -> result.drawable.toBitmap()
        else -> null
    }

    return bitmap?.let {
        withContext(Dispatchers.Default) {
            val palette = Palette.Builder(bitmap)
                // Disable any bitmap resizing in Palette. We've already loaded an appropriately
                // sized bitmap through Coil
                .resizeBitmapArea(0)
                // Clear any built-in filters. We want the unfiltered dominant color
                .clearFilters()
                // We reduce the maximum color count down to 8
                .maximumColorCount(8)
                .generate()
            // dominant
            // vibrant
            // muted
            palette
        }
    }
}*/