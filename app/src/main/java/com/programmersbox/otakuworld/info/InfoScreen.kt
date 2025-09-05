package com.programmersbox.otakuworld.info

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(viewModel: InfoViewModel = koinViewModel()) {
    LifecycleResumeEffect(Unit) {
        viewModel.checkForApps()
        onPauseOrDispose { }
    }

    var state by remember(viewModel.hasApps) { mutableIntStateOf(0) }

    val tabs by remember {
        derivedStateOf {
            listOfNotNull(
                OtakuItemState(
                    "MangaWorld",
                    viewModel.mangaWorld
                ).takeIf { viewModel.hasApps.hasMangaWorld },
                OtakuItemState(
                    "AnimeWorld",
                    viewModel.animeWorld
                ).takeIf { viewModel.hasApps.hasAnimeWorld },
                OtakuItemState(
                    "NovelWorld",
                    viewModel.novelWorld
                ).takeIf { viewModel.hasApps.hasNovelWorld },
            )
        }
    }

    val pagerState = rememberPagerState {
        tabs.size
    }

    LaunchedEffect(pagerState) {
        state = pagerState.currentPage
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OtakuWorld") },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PrimaryScrollableTabRow(
                selectedTabIndex = state,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state == index,
                        onClick = { state = index },
                        text = { Text(text = title.appName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            Text(viewModel.hasApps.toString())

            HorizontalPager(
                pagerState,
            ) {
                OtakuItemScreen(viewModel, tabs[it])
            }
        }
    }
}

@Composable
private fun OtakuItemScreen(
    viewModel: InfoViewModel,
    item: OtakuItemState,
) {
    Column {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            println(it)
        }

        Button(
            onClick = { launcher.launch(item.otakuItem.favoritePermission) },
        ) { Text("Allow Access to favorites ${item.appName}") }


        item.otakuItem.favorites.forEach {
            Text(it.toString())
        }
    }
}

data class OtakuItemState(
    val appName: String,
    val otakuItem: OtakuItem,
)