package com.programmersbox.uiviews.utils.sharedelements

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlin.reflect.KType

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedElementScope = staticCompositionLocalOf<SharedTransitionScope> { error("") }
val LocalNavigationAnimatedScope = staticCompositionLocalOf<AnimatedContentScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.customSharedElement(key: Any?) = composed {
    val scope = LocalSharedElementScope.current
    val animatedScope = LocalNavigationAnimatedScope.current

    if (animatedScope != null && key != null) {
        with(scope) {
            sharedElement(
                rememberSharedContentState(key), //TODO: Make this accept a class with more information!
                animatedScope,
            )
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