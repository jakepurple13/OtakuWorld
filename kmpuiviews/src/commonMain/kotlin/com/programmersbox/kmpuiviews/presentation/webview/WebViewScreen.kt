package com.programmersbox.kmpuiviews.presentation.webview

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
) {
    HideNavBarWhileOnScreen()
    val state = rememberWebViewState(url = url)

    DisposableEffect(Unit) {
        state.webSettings.apply {
            isJavaScriptEnabled = true
            androidWebSettings.apply {
                isAlgorithmicDarkeningAllowed = true
                safeBrowsingEnabled = true
            }
        }
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        url,
                        modifier = Modifier.basicMarquee()
                    )
                },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        WebView(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}