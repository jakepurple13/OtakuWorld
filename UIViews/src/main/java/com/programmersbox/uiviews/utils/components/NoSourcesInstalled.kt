package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExtensionOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.PreviewTheme
import com.programmersbox.uiviews.utils.Screen

@Composable
fun NoSourcesInstalled(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ExtensionOff,
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            Button(
                onClick = {
                    navController.navigate(Screen.ExtensionListScreen) {
                        popUpTo(Screen.SettingsScreen)
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Cannot find any extensions. Please install some!")
            }
        }
    }
}

@LightAndDarkPreviews
@Composable
private fun NoSourcesInstalledPreview() {
    PreviewTheme {
        NoSourcesInstalled()
    }
}