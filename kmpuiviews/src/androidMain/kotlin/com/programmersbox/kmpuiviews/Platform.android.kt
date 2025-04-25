package com.programmersbox.kmpuiviews

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.programmersbox.kmpuiviews.utils.navigateChromeCustomTabs

actual fun platform() = "Android"

@Composable
actual fun createColorScheme(
    darkTheme: Boolean,
    isExpressive: Boolean,
): ColorScheme {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> darkColorScheme(
            primary = Color(0xff90CAF9),
            secondary = Color(0xff90CAF9)
        )

        //TODO: On next cmp update, add expressive color scheme
        isExpressive -> lightColorScheme()//expressiveLightColorScheme()

        else -> lightColorScheme()
    }
}

actual fun customUriHandler(navController: NavHostController): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        navController.navigateChromeCustomTabs(
            url = uri,
            builder = {
                anim {
                    enter = R.anim.slide_in_right
                    popEnter = R.anim.slide_in_right
                    exit = R.anim.slide_out_left
                    popExit = R.anim.slide_out_left
                }
            }
        )
    }
}