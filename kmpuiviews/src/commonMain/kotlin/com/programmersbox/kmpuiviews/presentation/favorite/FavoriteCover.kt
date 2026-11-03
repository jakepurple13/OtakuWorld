package com.programmersbox.kmpuiviews.presentation.favorite

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import kotlin.time.Duration.Companion.milliseconds

/** Shared transition scope for the favorites screen (set by its SharedTransitionLayout). */
@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalFavoritesSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Scope of the Overview/Library tab AnimatedContent; covers fly between tabs through it. */
internal val LocalFavoritesTabScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Scope of the list/grid AnimatedContent; covers morph between views through it. */
internal val LocalFavoritesViewScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

internal val LocalFavoritesColorFilter = staticCompositionLocalOf<ColorFilter?> { null }

// Separate from OtakuSharedElement keys so the nav-level cover → Details transition is untouched.
private data class FavoriteCoverKey(val title: String, val layer: Int)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.favoriteCoverShared(title: String): Modifier {
    val shared = LocalFavoritesSharedScope.current ?: return this
    val tab = LocalFavoritesTabScope.current
    val view = LocalFavoritesViewScope.current
    var modifier = this
    with(shared) {
        if (tab != null) {
            modifier = modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(FavoriteCoverKey(title, 0)),
                animatedVisibilityScope = tab,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
            )
        }
        if (view != null) {
            modifier = modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(FavoriteCoverKey(title, 1)),
                animatedVisibilityScope = view,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
            )
        }
    }
    return modifier
}

/** A favorite group's cover, shared across tabs and list/grid by title. */
@Composable
internal fun FavoriteCover(
    group: FavoriteGroup,
    shape: Shape,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.favoriteCoverShared(group.title),
    ) {
        Box {
            ImageLoaderChoice(
                imageUrl = group.items.first().imageUrl,
                name = group.title,
                placeHolder = { rememberVectorPainter(Icons.Default.BrokenImage) },
                contentScale = ContentScale.Crop,
                colorFilter = LocalFavoritesColorFilter.current,
                modifier = Modifier.matchParentSize(),
            )
            overlay()
        }
    }
}

/** Compact age like "2d", "3w", "4mo", "1y". */
internal fun shortAge(createdAt: Long, now: Long): String {
    val days = (now - createdAt).milliseconds.inWholeDays.coerceAtLeast(0)
    return when {
        days < 7 -> "${days}d"
        days < 30 -> "${days / 7}w"
        days < 365 -> "${days / 30}mo"
        else -> "${days / 365}y"
    }
}
