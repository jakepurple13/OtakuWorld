package com.programmersbox.animeworldtv.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardLayoutDefaults
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.ImmersiveListScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.StandardCardLayout
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourceFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)
@Composable
fun MainView() {
    val vm = viewModel<MainViewModel>()
    val navController = LocalNavController.current

    TvLazyColumn {
        item {
            FeaturedMoviesCarousel(
                movies = vm.randomShows,
                goToVideoPlayer = {}
            )
        }

        vm.shows.forEach { (t, u) ->
            item {
                MoviesRow(
                    modifier = Modifier.padding(top = 16.dp),
                    movies = u,
                    title = t.toString(),
                    onMovieClick = { Screen.DetailScreen.navigateToDetails(navController, it) }
                )
            }
        }
    }

}


@OptIn(
    ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class
)
@Composable
fun FeaturedMoviesCarousel(
    movies: List<ItemModel>,
    goToVideoPlayer: () -> Unit
) {
    val carouselHeight = LocalConfiguration.current.screenHeightDp.dp.times(0.60f)
    var carouselCurrentIndex by rememberSaveable { mutableIntStateOf(0) }
    val carouselState = remember { CarouselState(initialActiveItemIndex = carouselCurrentIndex) }
    var isCarouselFocused by remember { mutableStateOf(false) }

    LaunchedEffect(carouselState.activeItemIndex) {
        carouselCurrentIndex = carouselState.activeItemIndex
    }

    val carouselBorderAlpha by animateFloatAsState(
        targetValue = if (isCarouselFocused) 1f else 0f,
        label = "",
    )

    AnimatedContent(
        targetState = movies,
        label = "Featured Carousel animation",
        modifier = Modifier
            .fillMaxWidth()
            .height(carouselHeight)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = carouselBorderAlpha),
                shape = ShapeDefaults.Medium,
            )
            .clip(ShapeDefaults.Medium)
            .onFocusChanged {
                // Because the carousel itself never gets the focus
                isCarouselFocused = it.hasFocus
            }
    ) { moviesState ->
        Carousel(
            modifier = Modifier
                .fillMaxSize()
                .handleDPadKeyEvents(onEnter = goToVideoPlayer),
            itemCount = moviesState.size,
            carouselState = carouselState,
            carouselIndicator = {
                Box(
                    modifier = Modifier
                        .padding(32.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        .graphicsLayer {
                            clip = true
                            shape = ShapeDefaults.ExtraSmall
                        }
                        .align(Alignment.BottomEnd)
                ) {
                    CarouselDefaults.IndicatorRow(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        itemCount = moviesState.size,
                        activeItemIndex = carouselState.activeItemIndex,
                    )
                }
            },
            contentTransformStartToEnd = fadeIn(tween(durationMillis = 1000))
                .togetherWith(fadeOut(tween(durationMillis = 1000))),
            contentTransformEndToStart = fadeIn(tween(durationMillis = 1000))
                .togetherWith(fadeOut(tween(durationMillis = 1000))),
            content = { index ->
                val movie = remember(index) { moviesState[index] }
                Box {
                    AsyncImage(
                        model = movie.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateEnterExit(
                            enter = slideInHorizontally(animationSpec = tween(1000)) { it / 2 },
                            exit = slideOutHorizontally(animationSpec = tween(1000))
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.displayMedium.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    offset = Offset(x = 2f, y = 4f),
                                    blurRadius = 2f
                                )
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = movie.description,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.65f
                                ),
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    offset = Offset(x = 2f, y = 4f),
                                    blurRadius = 2f
                                )
                            ),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        )
    }
}

enum class ItemDirection(val aspectRatio: Float) {
    Vertical(10.5f / 16f),
    Horizontal(16f / 9f);
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalTvFoundationApi::class
)
@Composable
fun MoviesRow(
    modifier: Modifier = Modifier,
    itemWidth: Dp = LocalConfiguration.current.screenWidthDp.dp.times(0.165f),
    itemDirection: ItemDirection = ItemDirection.Vertical,
    startPadding: Dp = rememberChildPadding().start,
    endPadding: Dp = rememberChildPadding().end,
    title: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp
    ),
    showItemTitle: Boolean = true,
    showIndexOverImage: Boolean = false,
    focusedItemIndex: (index: Int) -> Unit = {},
    movies: List<ItemModel>,
    onMovieClick: (movie: ItemModel) -> Unit = {}
) {
    Column(
        modifier = modifier.focusGroup()
    ) {
        title?.let { nnTitle ->
            Text(
                text = nnTitle,
                style = titleStyle,
                modifier = Modifier
                    .alpha(1f)
                    .padding(start = startPadding)
                    .padding(vertical = 16.dp)
            )
        }

        AnimatedContent(
            targetState = movies,
            label = "",
        ) { movieState ->
            FocusGroup {
                TvLazyRow(
                    pivotOffsets = PivotOffsets(parentFraction = 0.07f)
                ) {
                    item { Spacer(modifier = Modifier.padding(start = startPadding)) }

                    movieState.forEachIndexed { index, movie ->
                        item {
                            key(movie.hashCode()) {
                                MoviesRowItem(
                                    modifier = Modifier.restorableFocus(),
                                    focusedItemIndex = focusedItemIndex,
                                    index = index,
                                    itemWidth = itemWidth,
                                    itemDirection = itemDirection,
                                    onMovieClick = onMovieClick,
                                    movie = movie,
                                    showItemTitle = showItemTitle,
                                    showIndexOverImage = showIndexOverImage
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.padding(end = 20.dp)) }
                    }

                    item { Spacer(modifier = Modifier.padding(start = endPadding)) }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalTvFoundationApi::class,
    ExperimentalTvMaterial3Api::class
)
@Composable
fun ImmersiveListScope.ImmersiveListMoviesRow(
    modifier: Modifier = Modifier,
    itemWidth: Dp = LocalConfiguration.current.screenWidthDp.dp.times(0.165f),
    itemDirection: ItemDirection = ItemDirection.Vertical,
    startPadding: Dp = rememberChildPadding().start,
    endPadding: Dp = rememberChildPadding().end,
    title: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp
    ),
    showItemTitle: Boolean = true,
    showIndexOverImage: Boolean = false,
    focusedItemIndex: (index: Int) -> Unit = {},
    movies: List<ItemModel>,
    onMovieClick: (movie: ItemModel) -> Unit = {}
) {
    Column(
        modifier = modifier.focusGroup()
    ) {
        title?.let { nnTitle ->
            Text(
                text = nnTitle,
                style = titleStyle,
                modifier = Modifier
                    .alpha(1f)
                    .padding(start = startPadding)
                    .padding(vertical = 16.dp)
            )
        }

        AnimatedContent(
            targetState = movies,
            label = "",
        ) { movieState ->
            FocusGroup {
                TvLazyRow(
                    pivotOffsets = PivotOffsets(parentFraction = 0.07f)
                ) {
                    item { Spacer(modifier = Modifier.padding(start = startPadding)) }

                    movieState.forEachIndexed { index, movie ->
                        item {
                            key(movie.hashCode()) {
                                MoviesRowItem(
                                    modifier = Modifier
                                        .immersiveListItem(index)
                                        .restorableFocus(),
                                    focusedItemIndex = focusedItemIndex,
                                    index = index,
                                    itemWidth = itemWidth,
                                    itemDirection = itemDirection,
                                    onMovieClick = onMovieClick,
                                    movie = movie,
                                    showItemTitle = showItemTitle,
                                    showIndexOverImage = showIndexOverImage
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.padding(end = 20.dp)) }
                    }

                    item { Spacer(modifier = Modifier.padding(start = endPadding)) }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
private fun MoviesRowItem(
    modifier: Modifier = Modifier,
    focusedItemIndex: (index: Int) -> Unit,
    index: Int,
    itemWidth: Dp,
    itemDirection: ItemDirection,
    onMovieClick: (movie: ItemModel) -> Unit,
    movie: ItemModel,
    showItemTitle: Boolean,
    showIndexOverImage: Boolean
) {
    var isItemFocused by remember { mutableStateOf(false) }

    StandardCardLayout(
        modifier = Modifier
            .width(itemWidth)
            .onFocusChanged {
                isItemFocused = it.isFocused
                if (isItemFocused) {
                    focusedItemIndex(index)
                }
            }
            .focusProperties {
                if (index == 0) {
                    left = FocusRequester.Cancel
                }
            }
            .then(modifier),
        title = {
            MoviesRowItemText(
                showItemTitle = showItemTitle,
                isItemFocused = isItemFocused,
                movie = movie
            )
        },
        imageCard = {
            CardLayoutDefaults.ImageCard(
                onClick = { onMovieClick(movie) },
                shape = CardDefaults.shape(),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                ),
                scale = CardDefaults.scale(focusedScale = 1f),
                interactionSource = it,
                glow = CardDefaults.glow(glow = Glow(MaterialTheme.colorScheme.primary, 2.dp))
            ) {
                MoviesRowItemImage(
                    modifier = Modifier.aspectRatio(itemDirection.aspectRatio),
                    showIndexOverImage = showIndexOverImage,
                    movie = movie,
                    index = index
                )
            }
        },
    )
}

@Composable
private fun MoviesRowItemImage(
    modifier: Modifier = Modifier,
    showIndexOverImage: Boolean,
    movie: ItemModel,
    index: Int
) {
    Box(contentAlignment = Alignment.CenterStart) {
        AsyncImage(
            modifier = modifier
                .fillMaxWidth()
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        if (showIndexOverImage) {
                            drawRect(
                                color = Color.Black.copy(
                                    alpha = 0.1f
                                )
                            )
                        }
                    }
                },
            model = ImageRequest.Builder(LocalContext.current)
                .crossfade(true)
                .data(movie.imageUrl)
                .build(),
            contentDescription = "movie poster of ${movie.title}",
            contentScale = ContentScale.Crop
        )
        if (showIndexOverImage) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "#${index.inc()}",
                style = MaterialTheme.typography.displayLarge
                    .copy(
                        shadow = Shadow(
                            offset = Offset(0.5f, 0.5f),
                            blurRadius = 5f
                        ),
                        color = Color.White
                    ),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MoviesRowItemText(
    showItemTitle: Boolean,
    isItemFocused: Boolean,
    movie: ItemModel
) {
    if (showItemTitle) {
        val movieNameAlpha by animateFloatAsState(
            targetValue = if (isItemFocused) 1f else 0f,
            label = "",
        )
        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(movieNameAlpha)
                .fillMaxWidth()
                .padding(top = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

class MainViewModel : ViewModel() {

    val randomShows = mutableStateListOf<ItemModel>()
    val shows = mutableStateMapOf<Char?, List<ItemModel>>()

    init {
        sourceFlow
            .filterNotNull()
            .flowOn(Dispatchers.IO)
            .flatMapMerge { it.getListFlow() }
            .onEach {
                randomShows.clear()
                randomShows.addAll(it.randomN(10))
            }
            .map { items -> items.groupBy { it.title.firstOrNull() } }
            .onEach {
                shows.clear()
                shows.putAll(it)
            }
            .launchIn(viewModelScope)
    }

}

fun <T> Collection<T>.randomN(n: Int): List<T> = buildList { repeat(n) { add(this@randomN.random()) } }