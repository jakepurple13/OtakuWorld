package com.programmersbox.otakuworld.info

import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.SyncRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.programmersbox.otakuworld.BuildConfig
import com.skydoves.landscapist.glide.GlideImage
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(viewModel: InfoViewModel = koinViewModel()) {
    LifecycleResumeEffect(Unit) {
        viewModel.checkForApps()
        onPauseOrDispose { }
    }

    var state by remember(viewModel.hasApps) { mutableIntStateOf(0) }

    val tabsState by remember {
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

    val scrollState = rememberScrollState()

    val pagerState = rememberPagerState { tabsState.size }

    LaunchedEffect(pagerState.currentPage) {
        state = pagerState.currentPage
        scrollState.animateScrollTo(pagerState.currentPage)
    }

    LaunchedEffect(scrollState, state) {
        pagerState.animateScrollToPage(state)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OtakuWorld") },
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding)
        ) {
            if (viewModel.hasApps.let { it.hasMangaWorld || it.hasAnimeWorld || it.hasNovelWorld }) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = state,
                    scrollState = scrollState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabsState.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = { Text(text = title.appName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }

                val stateHolder = rememberSaveableStateHolder()

                HorizontalPager(
                    pagerState,
                    //modifier = Modifier.fillMaxSize()
                ) {
                    stateHolder.SaveableStateProvider(it) {
                        OtakuItemScreen(viewModel, tabsState[it])
                    }
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("No Apps Found")
                }
            }
        }
    }
}

@Composable
private fun OtakuItemScreen(
    viewModel: InfoViewModel,
    item: OtakuItemState,
) {
    var hasFavoritePermission by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasFavoritePermission = it }

    SideEffect { launcher.launch(item.otakuItem.favoritePermission) }

    Crossfade(hasFavoritePermission) { target ->
        if (target) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    //TODO: This all will go into a settings screen
                    val context = LocalContext.current

                    Button(
                        onClick = {
                            AccountManager.get(context)
                                .getAccountsByType(BuildConfig.ACCOUNT_TYPE)
                                .forEach { account ->
                                    println(account)
                                    ContentResolver.setSyncAutomatically(
                                        account,
                                        item.otakuItem.favoritesUri,
                                        true
                                    )
                                    ContentResolver.requestSync(
                                        SyncRequest.Builder()
                                            .setDisallowMetered(true)
                                            .setSyncAdapter(
                                                account,
                                                item.otakuItem.favoritesUri
                                            )
                                            .setExtras(
                                                bundleOf(
                                                    "type" to item.otakuItem.app.name
                                                )
                                            )
                                            .syncPeriodic(
                                                1.days.inWholeSeconds,
                                                1.hours.inWholeSeconds
                                            )
                                            .build()
                                    )
                                }
                        },
                    ) { Text("Setup Syncs") }
                }
                items(item.otakuItem.favorites) {
                    M3CoverCard(
                        imageUrl = it.imageUrl,
                        name = it.title,
                    )
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Button(
                    onClick = { launcher.launch(item.otakuItem.favoritePermission) },
                ) { Text("Allow Access to favorites ${item.appName}") }
            }
        }
    }
}

@Composable
fun M3CoverCard(
    imageUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .size(
                ComposableUtils.IMAGE_WIDTH,
                ComposableUtils.IMAGE_HEIGHT
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GlideImage(
                imageModel = { imageUrl },
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ),
                            startY = 50f
                        )
                    )
            ) {
                Text(
                    name,
                    style = MaterialTheme
                        .typography
                        .bodyLarge
                        .copy(textAlign = TextAlign.Center, color = Color.White),
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

data class OtakuItemState(
    val appName: String,
    val otakuItem: OtakuItem,
)

object ComposableUtils {
    const val IMAGE_WIDTH_PX = 360
    const val IMAGE_HEIGHT_PX = 480
    val IMAGE_WIDTH @Composable get() = with(LocalDensity.current) { IMAGE_WIDTH_PX.toDp() }
    val IMAGE_HEIGHT @Composable get() = with(LocalDensity.current) { IMAGE_HEIGHT_PX.toDp() }
}