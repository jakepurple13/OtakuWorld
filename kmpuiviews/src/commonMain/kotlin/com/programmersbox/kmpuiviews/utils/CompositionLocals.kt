package com.programmersbox.kmpuiviews.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.programmersbox.kmpuiviews.customUriHandler
import com.programmersbox.kmpuiviews.presentation.Screen

val LocalNavHostPadding = staticCompositionLocalOf<PaddingValues> { error("") }
val LocalNavController = staticCompositionLocalOf<NavHostController> { error("No NavController Found!") }

@Composable
fun KmpLocalCompositionSetup(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val defaultUriHandler = LocalUriHandler.current
    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalUriHandler provides remember {
            object : UriHandler {
                private val customHandler = customUriHandler(navController)

                override fun openUri(uri: String) {
                    runCatching { customHandler.openUri(uri) }
                        .onFailure { it.printStackTrace() }
                        .recoverCatching { defaultUriHandler.openUri(uri) }
                        .onFailure { it.printStackTrace() }
                        .onFailure { navController.navigate(Screen.WebViewScreen(uri)) }
                }
            }
        },
        content = content
    )
}