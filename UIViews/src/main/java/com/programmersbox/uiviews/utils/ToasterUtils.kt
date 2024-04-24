package com.programmersbox.uiviews.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterDefaults
import com.dokar.sonner.ToasterState


object ToasterUtils {
    const val LOADING_TOAST_ID = 1234
}

@Composable
fun ToasterSetup(
    toaster: ToasterState,
) {
    Toaster(
        state = toaster,
        richColors = true,
        iconSlot = { toast ->
            if (toast.icon == ToasterUtils.LOADING_TOAST_ID) {
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 1.5.dp,
                    )
                }
            } else {
                // Fallback to the default icon slot
                ToasterDefaults.iconSlot(toast)
            }
        },
        darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
                (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    )
}