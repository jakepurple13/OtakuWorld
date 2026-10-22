package com.programmersbox.kmpuiviews.presentation.tips

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.categorySetting
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    OtakuScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tips & Gestures") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        TipsContent(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
/**
 * Every tip, shared ones first, then the running app's. "Try" swaps the list for that tip's practice
 * view. [onPracticeChange] reports when a practice view is open so a pager can stop swiping.
 */
@Composable
fun TipsContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPracticeChange: (Boolean) -> Unit = {},
    header: @Composable () -> Unit = {},
    appConfig: AppConfig = koinInject(),
    composeSettingsDsl: ComposeSettingsDsl = koinInject(),
) {
    val appTips = remember(composeSettingsDsl) { buildTips(composeSettingsDsl.tips) }
    var practicing by remember { mutableStateOf<Tip?>(null) }

    val updatedOnPracticeChange by rememberUpdatedState(onPracticeChange)
    LaunchedEffect(practicing) { updatedOnPracticeChange(practicing != null) }
    DisposableEffect(Unit) { onDispose { updatedOnPracticeChange(false) } }

    BackHandler(practicing != null) { practicing = null }

    AnimatedContent(
        targetState = practicing,
        transitionSpec = {
            val towards = if (targetState != null) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            }
            slideIntoContainer(towards, tween(300)) + fadeIn() togetherWith
                slideOutOfContainer(towards, tween(300)) + fadeOut()
        },
        modifier = modifier,
    ) { tip ->
        if (tip == null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(bottom = 16.dp)
            ) {
                header()
                TipGroup("Everywhere", sharedTips) { practicing = it }
                if (appTips.isNotEmpty()) {
                    TipGroup(appConfig.appName, appTips) { practicing = it }
                }
            }
        } else {
            TipPractice(
                tip = tip,
                onBack = { practicing = null },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        }
    }
}

@Composable
private fun TipGroup(
    title: String,
    tips: List<Tip>,
    onTry: (Tip) -> Unit,
) {
    CategoryGroup {
        categorySetting { Text(title) }

        tips.forEach { tip ->
            item {
                ListItem(
                    headlineContent = { Text(tip.title) },
                    supportingContent = { Text(tip.description) },
                    leadingContent = { Icon(tip.gesture.icon, null) },
                    trailingContent = tip.demo?.let {
                        { FilledTonalButton(onClick = { onTry(tip) }) { Text("Try") } }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun TipPractice(
    tip: Tip,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var done by remember(tip) { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            Text("Tips", modifier = Modifier.padding(start = 8.dp))
        }

        Text(tip.title, style = MaterialTheme.typography.headlineSmall)
        Text(
            tip.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            tip.demo?.invoke { done = true }
        }

        AnimatedVisibility(
            visible = done,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Text("Got it", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
