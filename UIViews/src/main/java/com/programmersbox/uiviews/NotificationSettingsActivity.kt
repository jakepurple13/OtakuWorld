package com.programmersbox.uiviews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.programmersbox.kmpuiviews.notifier.InAppBannerHost
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettings
import com.programmersbox.kmpuiviews.theme.OtakuMaterialTheme
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import org.koin.android.ext.android.inject

class NotificationSettingsActivity : ComponentActivity() {
    private val settingsHandling: NewSettingsHandling by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OtakuMaterialTheme(
                settingsHandling = settingsHandling,
            ) {
                KmpLocalCompositionSetup {
                    CompositionLocalProvider(
                        LocalNavHostPadding provides PaddingValues(),
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            NotificationSettings()
                            InAppBannerHost(topEnd = false)
                        }
                    }
                }
            }
        }
    }
}