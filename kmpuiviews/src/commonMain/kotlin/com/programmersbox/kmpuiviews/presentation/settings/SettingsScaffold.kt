package com.programmersbox.kmpuiviews.presentation.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { BackButton() },
            scrollBehavior = it,
        )
    },
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    OtakuScaffold(
        topBar = { topBar(scrollBehavior) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        snackbarHost = snackbarHost,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { p ->
        Column(
            verticalArrangement = verticalArrangement,
            content = content,
            modifier = Modifier
                .padding(p)
                .animateContentSize()
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}