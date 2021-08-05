package com.programmersbox.uiviews

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.PlaceHolderCoverCard
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class GlobalSearchFragment : Fragment() {

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val info: GenericInfo by inject()
    private val logo: NotificationLogo by inject()
    private val mainLogo: MainLogo by inject()
    private val searchPublisher = BehaviorSubject.createDefault<List<ItemModel>>(emptyList())

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                MdcTheme {

                    var searchText by rememberSaveable { mutableStateOf("") }
                    val focusManager = LocalFocusManager.current
                    val listState = rememberLazyListState()
                    val scope = rememberCoroutineScope()
                    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = false)
                    val list by searchPublisher.subscribeAsState(initial = emptyList())
                    val networkState by ReactiveNetwork.observeInternetConnectivity()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeAsState(initial = true)

                    Scaffold(
                        topBar = {
                            Column(modifier = Modifier.padding(5.dp)) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    label = { Text(stringResource(id = R.string.search)) },
                                    trailingIcon = { IconButton(onClick = { searchText = "" }) { Icon(Icons.Default.Cancel, null) } },
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                                )
                            }
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { scope.launch { listState.animateScrollToItem(0) } }
                            ) { Icon(Icons.Default.VerticalAlignTop, null) }
                        },
                        floatingActionButtonPosition = FabPosition.End
                    ) {

                        if (networkState) {
                            SwipeRefresh(
                                state = swipeRefreshState,
                                onRefresh = {},
                                swipeEnabled = false,
                                modifier = Modifier.padding(it)
                            ) {
                                LazyVerticalGrid(cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH), state = listState) {
                                    if (swipeRefreshState.isRefreshing) {
                                        items(9) { PlaceHolderCoverCard(placeHolder = logo.notificationId) }
                                    } else if (list.isNotEmpty()) {
                                        items(list) { m ->
                                            SearchCoverCard(
                                                model = m,
                                                placeHolder = AppCompatResources.getDrawable(LocalContext.current, mainLogo.logoId)
                                            ) {
                                                findNavController()
                                                    .navigate(GlobalSearchFragmentDirections.actionGlobalSearchFragmentToDetailsFragment(m))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(it),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    Icons.Default.CloudOff,
                                    null,
                                    modifier = Modifier.size(50.dp, 50.dp),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground)
                                )
                                Text(stringResource(R.string.you_re_offline), style = MaterialTheme.typography.h5)
                            }
                        }
                    }

                    LaunchedEffect(key1 = searchText) {
                        snapshotFlow { searchText }
                            .debounce(1000)
                            .distinctUntilChanged()
                            .collect { s ->
                                println("Searching for $s")

                                Observable.combineLatest(
                                    info.searchList()
                                        .map {
                                            it
                                                .searchList(s, list = emptyList())
                                                .timeout(5, TimeUnit.SECONDS)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .onErrorReturnItem(emptyList())
                                                .toObservable()
                                        }
                                ) { (it as Array<List<ItemModel>>).toList().flatten().sortedBy(ItemModel::title) }
                                    .doOnSubscribe { swipeRefreshState.isRefreshing = true }
                                    .onErrorReturnItem(emptyList())
                                    .subscribe {
                                        searchPublisher.onNext(it)
                                        swipeRefreshState.isRefreshing = false
                                    }
                                    .addTo(disposable)
                            }
                    }
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    @ExperimentalMaterialApi
    @Composable
    fun SearchCoverCard(model: ItemModel, placeHolder: Drawable?, error: Drawable? = placeHolder, onClick: () -> Unit = {}) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .padding(5.dp)
                .size(
                    ComposableUtils.IMAGE_WIDTH,
                    ComposableUtils.IMAGE_HEIGHT
                ),
            indication = rememberRipple(),
            onClickLabel = model.title,
        ) {

            Column {

                Text(
                    model.source.serviceName,
                    style = MaterialTheme
                        .typography
                        .body1
                        .copy(textAlign = TextAlign.Center),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                )

                Box {
                    GlideImage(
                        imageModel = model.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        requestBuilder = Glide.with(LocalView.current)
                            .asBitmap()
                            //.override(360, 480)
                            .placeholder(placeHolder)
                            .error(error)
                            .fallback(placeHolder)
                            .transform(RoundedCorners(5)),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                        loading = {
                            Image(
                                painter = rememberDrawablePainter(drawable = placeHolder),
                                contentDescription = model.title,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            )
                        },
                        failure = {
                            Image(
                                painter = rememberDrawablePainter(drawable = error),
                                contentDescription = model.title,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            )
                        }
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
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            model.title,
                            style = MaterialTheme
                                .typography
                                .body1
                                .copy(textAlign = TextAlign.Center, color = Color.White),
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }

        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = GlobalSearchFragment()
    }
}