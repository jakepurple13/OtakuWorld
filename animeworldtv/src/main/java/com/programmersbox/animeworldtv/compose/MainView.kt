@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.programmersbox.animeworldtv.compose

import androidx.compose.runtime.Composable
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.programmersbox.models.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)
@Composable
fun MainView() {
    /*val currentSourceRepository = LocalCurrentSource.current
    val vm = viewModel { MainViewModel(currentSourceRepository) }
    val navController = LocalNavController.current

    LazyColumn {
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
    }*/
}


/*
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

class MainViewModel(
    currentSourceRepository: CurrentSourceRepository,
) : ViewModel() {

    val randomShows = mutableStateListOf<ItemModel>()
    val shows = mutableStateMapOf<Char?, List<ItemModel>>()

    init {
        currentSourceRepository.asFlow()
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
*/

class CurrentSourceRepository {
    private val sourceFlow = MutableStateFlow<ApiService?>(null)

    suspend fun emit(apiService: ApiService?) {
        sourceFlow.emit(apiService)
    }

    fun tryEmit(apiService: ApiService?) {
        sourceFlow.tryEmit(apiService)
    }

    fun asFlow() = sourceFlow.asStateFlow()

}