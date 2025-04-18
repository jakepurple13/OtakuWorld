package com.programmersbox.uiviews.presentation.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState
import com.programmersbox.uiviews.utils.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
) {
    val state = rememberWebViewState(url = url)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(url) },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        WebView(
            state = state,
            onCreated = { it.settings.javaScriptEnabled = true },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}