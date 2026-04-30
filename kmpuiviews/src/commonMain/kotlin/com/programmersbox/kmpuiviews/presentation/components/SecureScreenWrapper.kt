package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import com.programmersbox.kmpuiviews.HideScreen

@Composable
fun SecureScreenWrapper(
    shouldSecure: Boolean,
    content: @Composable () -> Unit,
) {
    HideScreen(shouldHide = shouldSecure)

    val windowInfo = LocalWindowInfo.current.isWindowFocused

    Box(modifier = Modifier.fillMaxSize()) {
        // Draw the actual screen content
        content()

        // Draw the privacy overlay if the app loses focus
        AnimatedVisibility(
            !windowInfo && shouldSecure,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Nice Try!") },
                        navigationIcon = { BackButton() }
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}