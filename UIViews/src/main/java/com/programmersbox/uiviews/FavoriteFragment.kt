package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.models.ApiService
import com.programmersbox.rxutils.toLatestFlowable
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import androidx.compose.material3.MaterialTheme as M3MaterialTheme

class FavoriteFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = FavoriteFragment()
    }

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val disposable = CompositeDisposable()

    private val genericInfo by inject<GenericInfo>()
    private val logo: MainLogo by inject()
    private val fireListener = FirebaseDb.FirebaseListener()

    private val favoriteList = PublishSubject.create<List<DbModel>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Flowables.combineLatest(
            fireListener.getAllShowsFlowable(),
            dao.getAllFavorites()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        ) { fire, db -> (db + fire).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
            .replay(1)
            .refCount(1, TimeUnit.SECONDS)
            .subscribe(favoriteList::onNext)
            .addTo(disposable)
    }

    @ExperimentalMaterial3Api
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
        setContent {
            M3MaterialTheme(currentColorScheme) { FavoriteUi(favoriteItems = favoriteList.toLatestFlowable(), allSources = genericInfo.sourceList()) }
        }
    }

    override fun onDestroy() {
        disposable.dispose()
        fireListener.unregister()
        super.onDestroy()
    }

    @ExperimentalMaterial3Api
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    fun FavoriteUi(favoriteItems: Flowable<List<DbModel>>, allSources: List<ApiService>) {

        val focusManager = LocalFocusManager.current

        var searchText by rememberSaveable { mutableStateOf("") }

        val favorites by favoriteItems
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(initial = emptyList())

        val selectedSources = rememberMutableStateListOf(*allSources.fastMap(ApiService::serviceName).toTypedArray())

        val showing = favorites.filter { it.title.contains(searchText, true) && it.source in selectedSources }

        var sortedBy by remember { mutableStateOf<SortFavoritesBy<*>>(SortFavoritesBy.TITLE) }
        var reverse by remember { mutableStateOf(false) }

        var showBanner by remember { mutableStateOf(false) }

        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

        M3OtakuBannerBox(
            showBanner = showBanner,
            placeholder = logo.logoId
        ) { itemInfo ->
            CollapsingToolbarScaffold(
                modifier = Modifier,
                state = rememberCollapsingToolbarScaffoldState(),
                scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
                toolbar = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.background(TopAppBarDefaults.smallTopAppBarColors().containerColor(scrollBehavior.scrollFraction).value)
                    ) {
                        SmallTopAppBar(
                            scrollBehavior = scrollBehavior,
                            navigationIcon = { IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.Close, null) } },
                            title = { Text(stringResource(R.string.viewFavoritesMenu)) },
                            actions = {

                                val rotateIcon: @Composable (SortFavoritesBy<*>) -> Float = {
                                    animateFloatAsState(if (it == sortedBy && reverse) 180f else 0f).value
                                }

                                GroupButton(
                                    selected = sortedBy,
                                    options = listOf(
                                        GroupButtonModel(SortFavoritesBy.TITLE) {
                                            Icon(
                                                Icons.Default.SortByAlpha,
                                                null,
                                                modifier = Modifier.rotate(rotateIcon(SortFavoritesBy.TITLE))
                                            )
                                        },
                                        GroupButtonModel(SortFavoritesBy.COUNT) {
                                            Icon(
                                                Icons.Default.Sort,
                                                null,
                                                modifier = Modifier.rotate(rotateIcon(SortFavoritesBy.COUNT))
                                            )
                                        },
                                        GroupButtonModel(SortFavoritesBy.CHAPTERS) {
                                            Icon(
                                                Icons.Default.ReadMore,
                                                null,
                                                modifier = Modifier.rotate(rotateIcon(SortFavoritesBy.CHAPTERS))
                                            )
                                        }
                                    )
                                ) { if (sortedBy != it) sortedBy = it else reverse = !reverse }
                            }
                        )

                        MdcTheme {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                label = {
                                    androidx.compose.material.Text(
                                        resources.getQuantityString(
                                            R.plurals.numFavorites,
                                            showing.size,
                                            showing.size
                                        )
                                    )
                                },
                                trailingIcon = {
                                    androidx.compose.material.IconButton(onClick = { searchText = "" }) {
                                        androidx.compose.material.Icon(Icons.Default.Cancel, null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 5.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                        ) {

                            item {
                                CustomChip(
                                    "ALL",
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                selectedSources.clear()
                                                selectedSources.addAll(allSources.fastMap(ApiService::serviceName))
                                            },
                                            onLongClick = { selectedSources.clear() }
                                        ),
                                    backgroundColor = M3MaterialTheme.colorScheme.primary,
                                    textColor = M3MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            items(
                                (allSources.fastMap(ApiService::serviceName) + showing.fastMap(DbModel::source))
                                    .groupBy { it }
                                    .toList()
                                    .sortedBy { it.first }
                            ) {
                                CustomChip(
                                    "${it.first}: ${it.second.size - 1}",
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (it.first in selectedSources) selectedSources.remove(it.first)
                                                else selectedSources.add(it.first)
                                            },
                                            onLongClick = {
                                                selectedSources.clear()
                                                selectedSources.add(it.first)
                                            }
                                        ),
                                    backgroundColor = animateColorAsState(
                                        if (it.first in selectedSources) M3MaterialTheme.colorScheme.primary
                                        else M3MaterialTheme.colorScheme.surface
                                    ).value,
                                    textColor = animateColorAsState(
                                        if (it.first in selectedSources) M3MaterialTheme.colorScheme.onPrimary
                                        else M3MaterialTheme.colorScheme.onSurface
                                    ).value
                                )
                            }
                        }
                    }
                }
            ) {
                Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) { p ->
                    if (showing.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(p)
                        ) {

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                                tonalElevation = 5.dp,
                                shape = RoundedCornerShape(5.dp)
                            ) {

                                Column(modifier = Modifier) {

                                    Text(
                                        text = stringResource(id = R.string.get_started),
                                        style = M3MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Text(
                                        text = stringResource(R.string.get_started_info),
                                        style = M3MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Button(
                                        onClick = { (activity as? BaseMainActivity)?.goToScreen(BaseMainActivity.Screen.RECENT) },
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(vertical = 5.dp)
                                    ) { Text(text = stringResource(R.string.add_a_favorite)) }

                                }

                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
                            contentPadding = p,
                            state = rememberLazyListState()
                        ) {
                            items(
                                showing
                                    .groupBy(DbModel::title)
                                    .entries
                                    .let {
                                        when (val s = sortedBy) {
                                            is SortFavoritesBy.TITLE -> it.sortedBy(s.sort)
                                            is SortFavoritesBy.COUNT -> it.sortedByDescending(s.sort)
                                            is SortFavoritesBy.CHAPTERS -> it.sortedByDescending(s.sort)
                                        }
                                    }
                                    .let { if (reverse) it.reversed() else it }
                                    .toTypedArray()
                            ) { info ->
                                M3CoverCard(
                                    onLongPress = { c ->
                                        itemInfo.value = if (c == ComponentState.Pressed) {
                                            info.value.randomOrNull()
                                                ?.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                                        } else null
                                        showBanner = c == ComponentState.Pressed
                                    },
                                    imageUrl = remember { info.value.random().imageUrl },
                                    name = info.key,
                                    placeHolder = logo.logoId,
                                    favoriteIcon = {
                                        if (info.value.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Circle,
                                                    contentDescription = null,
                                                    tint = M3MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                                Text(
                                                    info.value.size.toString(),
                                                    color = M3MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    if (info.value.size == 1) {
                                        val item = info.value
                                            .firstOrNull()
                                            ?.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                                        findNavController().navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                                    } else {
                                        ListBottomSheet(
                                            title = getString(R.string.chooseASource),
                                            list = info.value,
                                            onClick = { item ->
                                                val i = item
                                                    .let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                                                findNavController()
                                                    .navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(i))
                                            }
                                        ) {
                                            ListBottomSheetItemModel(
                                                primaryText = it.title,
                                                overlineText = it.source
                                            )
                                        }.show(parentFragmentManager, "sourceChooser")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    sealed class SortFavoritesBy<K>(val sort: (Map.Entry<String, List<DbModel>>) -> K) {
        object TITLE : SortFavoritesBy<String>(Map.Entry<String, List<DbModel>>::key)
        object COUNT : SortFavoritesBy<Int>({ it.value.size })
        object CHAPTERS : SortFavoritesBy<Int>({ it.value.maxOf(DbModel::numChapters) })
    }

}