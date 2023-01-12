package com.programmersbox.uiviews.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.navigation.*
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import java.net.URLEncoder

@ExperimentalMaterialNavigationApi
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberBottomSheetNavigator(
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    skipHalfExpanded: Boolean = false,
): BottomSheetNavigator {
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        animationSpec,
        skipHalfExpanded = skipHalfExpanded,
    )
    return remember(sheetState) { BottomSheetNavigator(sheetState = sheetState) }
}


@Navigator.Name("chrome")
class ChromeCustomTabsNavigator(
    private val context: Context
) : Navigator<ChromeCustomTabsNavigator.Destination>() {

    override fun createDestination() = Destination(this)

    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): NavDestination? {
        val url = checkNotNull(args!!.getString(KEY_URL)) {
            "Destination ${destination.id} does not have an url."
        }
        val customTabsIntent = CustomTabsIntent.Builder().apply {
            if (navigatorExtras is Extras) {
                buildCustomTabsIntent(navigatorExtras)
            }
        }.build()
        customTabsIntent.launchUrl(context, url.toUri())
        return null // Do not add to the back stack, managed by Chrome Custom Tabs
    }

    override fun popBackStack() = true // Managed by Chrome Custom Tabs

    private fun CustomTabsIntent.Builder.buildCustomTabsIntent(
        extras: Extras
    ): CustomTabsIntent {
        val colorBuilder = CustomTabColorSchemeParams.Builder()
        if (extras.toolbarColor != null) {
            colorBuilder.setToolbarColor(extras.toolbarColor)
        }
        setDefaultColorSchemeParams(colorBuilder.build())

        setShareState(CustomTabsIntent.SHARE_STATE_ON)

        val customTabsIntent = build()

        // Adding referrer so websites know where their traffic came from, per Google's recommendations:
        // https://medium.com/google-developers/best-practices-for-custom-tabs-5700e55143ee
        customTabsIntent.intent.putExtra(
            Intent.EXTRA_REFERRER,
            Uri.parse("android-app://" + context.packageName)
        )

        return customTabsIntent
    }

    @NavDestination.ClassType(Activity::class)
    class Destination(navigator: ChromeCustomTabsNavigator) : NavDestination(navigator)

    class Extras internal constructor(
        internal val toolbarColor: Int?
    ) : Navigator.Extras {
        class Builder {
            @ColorInt
            private var _toolbarColor: Int? = null

            fun setToolbarColor(@ColorInt color: Int): Builder = apply {
                _toolbarColor = color
            }

            fun build(): Extras {
                return Extras(_toolbarColor)
            }
        }
    }

    companion object {
        internal const val KEY_URL = "url"
        internal const val KEY_ROUTE = "android-support-nav:controller:chrome"
    }
}

fun NavGraphBuilder.chromeCustomTabs() {
    addDestination(
        ChromeCustomTabsNavigator.Destination(
            provider[ChromeCustomTabsNavigator::class]
        ).apply {
            val route = "chrome/{${ChromeCustomTabsNavigator.KEY_URL}}"
            val internalRoute = createRoute(route)
            addDeepLink(internalRoute)
            addArgument(
                ChromeCustomTabsNavigator.KEY_ROUTE, navArgument(ChromeCustomTabsNavigator.KEY_ROUTE) { defaultValue = route }.argument
            )
            id = internalRoute.hashCode()
            addArgument(
                ChromeCustomTabsNavigator.KEY_URL, navArgument(ChromeCustomTabsNavigator.KEY_URL) { type = NavType.StringType }.argument
            )
        }
    )
}

fun NavController.navigateChromeCustomTabs(
    url: String,
    builder: NavOptionsBuilder.() -> Unit = {},
    extraBuilder: ChromeCustomTabsNavigator.Extras.Builder.() -> Unit = { },
) {
    navigate(
        NavDeepLinkRequest.Builder.fromUri(
            createRoute("chrome/${URLEncoder.encode(url, "utf-8")}").toUri()
        ).build(),
        navOptions(builder),
        ChromeCustomTabsNavigator.Extras.Builder().apply {
            extraBuilder()
        }.build()
    )
}

internal fun createRoute(route: String) = "android-app://androidx.navigation.chrome/$route"