@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.programmersbox.animeworldtv.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.programmersbox.anime_sources.anime.AllAnime.dispatchIo
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)
@Composable
fun DetailView() {
    val vm = viewModel { DetailViewModel(createSavedStateHandle()) }
    val navController = LocalNavController.current
    val childPadding = rememberChildPadding()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    BackHandler { navController.popBackStack() }

    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
    ) {
        item {
            vm.info?.let {
                MovieDetails(
                    movieDetails = it,
                    goToMoviePlayer = {}
                )
            }
        }

        item {
            FocusGroup {
                TvLazyRow(
                    pivotOffsets = PivotOffsets(parentFraction = 0.07f)
                ) {

                    items(vm.info?.chapters.orEmpty()) {
                        Card(onClick = {}) {
                            Text(it.name)
                        }
                    }

                    /*item { Spacer(modifier = Modifier.padding(start = startPadding)) }

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

                    item { Spacer(modifier = Modifier.padding(start = endPadding)) }*/
                }
            }
            /*MoviesRow(
                title = vm.info?.title.orEmpty(),
                titleStyle = MaterialTheme.typography.titleMedium,
                movies = vm.info?.chapters.orEmpty(),
                onMovieClick = {}
            )*/
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = childPadding.start)
                    .padding(BottomDividerPadding)
                    .fillMaxWidth()
                    .height(1.dp)
                    .alpha(0.15f)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = screenHeight.times(0.25f)))
        }
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetails(
    movieDetails: InfoModel,
    goToMoviePlayer: () -> Unit
) {
    val screenConfiguration = LocalConfiguration.current
    val screenHeight = screenConfiguration.screenHeightDp
    val childPadding = rememberChildPadding()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(screenHeight.dp.times(0.8f))
    ) {
        MovieImageWithGradients(
            movieDetails = movieDetails,
            bringIntoViewRequester = bringIntoViewRequester
        )

        Column(modifier = Modifier.fillMaxWidth(0.55f)) {
            Spacer(modifier = Modifier.height(screenHeight.dp.times(0.2f)))

            Column(
                modifier = Modifier.padding(start = childPadding.start)
            ) {
                MovieLargeTitle(movieTitle = movieDetails.title)

                Column(
                    modifier = Modifier.alpha(0.75f)
                ) {
                    MovieDescription(description = movieDetails.description)
                    FlowRow(
                        modifier = Modifier.padding(top = 20.dp),
                    ) {
                        movieDetails.genres.forEach {
                            Text(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieDescription(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        ),
        modifier = Modifier.padding(top = 8.dp),
        maxLines = 2
    )
}

@Composable
private fun MovieLargeTitle(movieTitle: String) {
    Text(
        text = movieTitle,
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        maxLines = 1
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun MovieImageWithGradients(
    movieDetails: InfoModel,
    bringIntoViewRequester: BringIntoViewRequester
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(movieDetails.imageUrl)
            .crossfade(true).build(),
        contentDescription = movieDetails.title,
        modifier = Modifier
            .fillMaxSize()
            .bringIntoViewRequester(bringIntoViewRequester),
        contentScale = ContentScale.Crop
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface
                    ),
                    startY = 600f
                )
            )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        Color.Transparent
                    ),
                    endX = 1000f,
                    startX = 300f
                )
            )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        Color.Transparent
                    ),
                    start = Offset(x = 500f, y = 500f),
                    end = Offset(x = 1000f, y = 0f)
                )
            )
    )
}

@Composable
fun TitleValueText(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.alpha(0.75f),
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            maxLines = 3
        )
    }
}

class DetailViewModel(
    handle: SavedStateHandle,
    itemModel: ItemModel? = handle.get<String>("model")
        ?.fromJson<ItemModel>(ApiService::class.java to ApiServiceDeserializer())
) : ViewModel() {

    var info: InfoModel? by mutableStateOf(null)

    var description: String by mutableStateOf("")

    init {
        itemModel?.toInfoModel()
            ?.dispatchIo()
            ?.catch { it.printStackTrace() }
            ?.onEach {
                if (it.isSuccess) {
                    info = it.getOrThrow()
                    description = it.getOrThrow().description
                }
            }
            ?.launchIn(viewModelScope)
    }

}