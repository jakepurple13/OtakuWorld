package com.programmersbox.kmpuiviews.utils.composables.sharedelements

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KType

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedElementScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalNavigationAnimatedScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

// Titles leave the cover's bounds on the way to Details, so they must not inherit the parent clip
@OptIn(ExperimentalSharedTransitionApi::class)
private val NoOverlayClip = object : SharedTransitionScope.OverlayClip {
    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Path? = null
}

/**
 * Covers move and resize, swapping from the source copy to the destination copy late in the flight.
 * Titles crossfade between their two text styles while their bounds move ([SharedTransitionScope.sharedBounds]).
 * No-op outside a navigation entry or when [key] is null.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.customSharedElement(
    key: OtakuSharedElement?,
) = composed {
    val scope = LocalSharedElementScope.current
    val animatedScope = LocalNavigationAnimatedScope.current

    if (animatedScope != null && key != null && scope != null) {
        with(scope) {
            when (key.type) {
                // The destination copy of the cover is usually still loading when the flight starts,
                // so keep the already-loaded source visible and swap near the end
                OtakuSharedElementType.Image -> sharedBounds(
                    sharedContentState = rememberSharedContentState(key = key),
                    animatedVisibilityScope = animatedScope,
                    enter = fadeIn(tween(durationMillis = 150, delayMillis = 250)),
                    exit = fadeOut(tween(durationMillis = 150, delayMillis = 250)),
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                )

                OtakuSharedElementType.Title -> sharedBounds(
                    sharedContentState = rememberSharedContentState(key = key),
                    animatedVisibilityScope = animatedScope,
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                    zIndexInOverlay = 1f,
                    clipInOverlayDuringTransition = NoOverlayClip,
                )
            }
        }
    } else {
        this
    }
}

public inline fun <reified T : Any> NavGraphBuilder.animatedScopeComposable(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards EnterTransition?)? = null,
    noinline exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards ExitTransition?)? = null,
    noinline popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards EnterTransition?)? = enterTransition,
    noinline popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards ExitTransition?)? = exitTransition,
    noinline sizeTransform: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards SizeTransform?)? = null,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T>(
    typeMap = typeMap,
    deepLinks = deepLinks,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    popEnterTransition = popEnterTransition,
    popExitTransition = popExitTransition,
    sizeTransform = sizeTransform,
) {
    CompositionLocalProvider(LocalNavigationAnimatedScope provides this) { content(it) }
}

public fun NavGraphBuilder.animatedScopeComposable(
    route: String,
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards EnterTransition?)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards ExitTransition?)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards EnterTransition?)? = enterTransition,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards ExitTransition?)? = exitTransition,
    sizeTransform: (AnimatedContentTransitionScope<NavBackStackEntry>.() ->
    @JvmSuppressWildcards SizeTransform?)? = null,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    deepLinks = deepLinks,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    popEnterTransition = popEnterTransition,
    popExitTransition = popExitTransition,
    sizeTransform = sizeTransform,
) {
    CompositionLocalProvider(LocalNavigationAnimatedScope provides this) { content(it) }
}