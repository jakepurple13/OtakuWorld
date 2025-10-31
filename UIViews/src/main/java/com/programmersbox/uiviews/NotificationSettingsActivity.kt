package com.programmersbox.uiviews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.navactions.TopLevelBackStack
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettings
import com.programmersbox.kmpuiviews.theme.OtakuMaterialTheme
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import org.koin.android.ext.android.inject

class NotificationSettingsActivity : ComponentActivity() {
    private val settingsHandling: NewSettingsHandling by inject()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OtakuMaterialTheme(
                navController = rememberNavController(),
                navBackStack = TopLevelBackStack(Screen.NotificationsSettings),
                settingsHandling = settingsHandling,
            ) {
                CompositionLocalProvider(
                    LocalNavHostPadding provides PaddingValues(),
                ) {
                    //FIXME: Need to fix the back button crash since right now it always does the nav action,
                    // but we should do built in back action
                    NotificationSettings()
                }
            }
        }
    }
}