package com.programmersbox.uiviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMaxBy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.models.ApiService
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.CoverCard
import com.programmersbox.uiviews.utils.CustomChip
import com.programmersbox.uiviews.utils.rememberMutableStateListOf
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.schedulers.Schedulers
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

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

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))

        val dbFire = Flowables.combineLatest(
            fireListener.getAllShowsFlowable(),
            dao.getAllFavorites()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        ) { fire, db -> (db + fire).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
            .replay(1)
            .refCount(1, TimeUnit.SECONDS)

        setContent { MdcTheme { FavoriteUi(favoriteItems = dbFire, allSources = genericInfo.sourceList()) } }
    }

    override fun onDestroy() {
        disposable.dispose()
        fireListener.unregister()
        super.onDestroy()
    }

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

        val selectedSources = rememberMutableStateListOf(*allSources.map { it.serviceName }.toTypedArray())

        val showing = favorites
            .filter { it.title.contains(searchText, true) && it.source in selectedSources }

        CollapsingToolbarScaffold(
            modifier = Modifier,
            state = rememberCollapsingToolbarScaffoldState(),
            scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
            toolbar = {
                Column(
                    modifier = Modifier.padding(5.dp)
                ) {

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text(resources.getQuantityString(R.plurals.numFavorites, showing.size, showing.size)) },
                        trailingIcon = { IconButton(onClick = { searchText = "" }) { Icon(Icons.Default.Cancel, null) } },
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )

                    Row(
                        modifier = Modifier.padding(top = 5.dp)
                    ) {

                        LazyRow {

                            item {
                                CustomChip(
                                    "ALL",
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                selectedSources.clear()
                                                selectedSources.addAll(allSources.map { it.serviceName })
                                            },
                                            onLongClick = {
                                                selectedSources.clear()
                                            }
                                        ),
                                    backgroundColor = MaterialTheme.colors.primary,
                                    textColor = MaterialTheme.colors.onPrimary
                                )
                            }

                            items(
                                (allSources.map { it.serviceName } + showing.map { it.source })
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
                                    backgroundColor = animateColorAsState(if (it.first in selectedSources) MaterialTheme.colors.primary else MaterialTheme.colors.surface).value,
                                    textColor = animateColorAsState(if (it.first in selectedSources) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface).value
                                )
                            }
                        }

                    }
                }
            }
        ) {

            Scaffold { p ->

                if (showing.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(p)
                    ) {

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            elevation = 5.dp,
                            shape = RoundedCornerShape(5.dp)
                        ) {

                            Column(modifier = Modifier) {

                                Text(
                                    text = stringResource(id = R.string.get_started),
                                    style = MaterialTheme.typography.h4,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Text(
                                    text = stringResource(R.string.get_started_info),
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Button(
                                    onClick = { (activity as? BaseMainActivity)?.goToScreen(BaseMainActivity.Screen.RECENT) },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(vertical = 5.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.add_a_favorite),
                                        style = MaterialTheme.typography.button
                                    )
                                }

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
                                .sortedBy { it.key }
                                .toTypedArray()
                        ) { info ->

                            var chooseSource by remember { mutableStateOf(false) }

                            if (chooseSource) {
                                AlertDialog(
                                    onDismissRequest = { chooseSource = false },
                                    text = {
                                        LazyColumn {
                                            items(info.value) { item ->
                                                Card(
                                                    onClick = {
                                                        chooseSource = false
                                                        val i = item.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                                                        findNavController()
                                                            .navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(i))
                                                    },
                                                    modifier = Modifier.padding(vertical = 5.dp)
                                                ) {
                                                    ListItem(
                                                        text = { Text(item.title) },
                                                        overlineText = { Text(item.source) }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    title = { Text(stringResource(R.string.chooseASource)) },
                                    confirmButton = { TextButton(onClick = { chooseSource = false }) { Text(stringResource(R.string.ok)) } }
                                )
                            }

                            CoverCard(
                                imageUrl = info.value.random().imageUrl,
                                name = info.key,
                                placeHolder = logo.logoId
                            ) {
                                if (info.value.size == 1) {
                                    val item = info.value.firstOrNull()?.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                                    findNavController().navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                                } else {
                                    chooseSource = true
                                }
                            }
                        }
                    }
                }
            }
        }

    }

}