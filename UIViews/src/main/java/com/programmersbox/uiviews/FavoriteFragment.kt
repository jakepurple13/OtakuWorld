package com.programmersbox.uiviews

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.chip.Chip
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeDiffUtil
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.helpfulutils.gone
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.visible
import com.programmersbox.models.ApiService
import com.programmersbox.rxutils.behaviorDelegate
import com.programmersbox.rxutils.toLatestFlowable
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.databinding.FavoriteItemBinding
import com.programmersbox.uiviews.databinding.FragmentFavoriteBinding
import com.programmersbox.uiviews.utils.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class FavoriteFragment : BaseFragment() {

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val disposable = CompositeDisposable()

    private val genericInfo by inject<GenericInfo>()
    private val sources by lazy { genericInfo.sourceList() }
    private val sourcePublisher = BehaviorSubject.createDefault(sources.toMutableList())
    private var sourcesList by behaviorDelegate(sourcePublisher)
    private val adapter by lazy { FavoriteAdapter() }

    private val logo: MainLogo by inject()
    private val logo2: NotificationLogo by inject()

    private val fireListener = FirebaseDb.FirebaseListener()

    override val layoutId: Int get() = R.layout.fragment_favorite

    private val COMPOSE_ONLY = false

    private lateinit var binding: FragmentFavoriteBinding

    /*override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }*/

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @SuppressLint("SetTextI18n")
    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
    //override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFavoriteBinding.bind(view)

        if (COMPOSE_ONLY) {
            binding.composeVersion.visible()
            binding.xmlVersion.gone()
            binding.favAppBar.gone()
        }

        uiSetup()

        val dbFire = Flowables.combineLatest(
            fireListener.getAllShowsFlowable(),
            dao.getAllFavorites()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        ) { fire, db -> (db + fire).groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! } }
            .replay(1)
            .refCount(1, TimeUnit.SECONDS)

        if (COMPOSE_ONLY) {
            binding.composeVersion.setContent { MdcTheme { MainUi(favoriteItems = dbFire, allSources = genericInfo.sourceList()) } }
        }

        if (!COMPOSE_ONLY) {

            Flowables.combineLatest(
                source1 = dbFire
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()),
                source2 = sourcePublisher.toLatestFlowable(),
                source3 = binding.favSearchInfo
                    .textChanges()
                    .debounce(500, TimeUnit.MILLISECONDS)
                    .toLatestFlowable()
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { pair ->
                    pair.first.sortedBy(DbModel::title)
                        .filter { it.source in pair.second.fastMap(ApiService::serviceName) && it.title.contains(pair.third, true) }
                }
                .map { it.size to it.toGroup() }
                .distinctUntilChanged()
                .subscribe {
                    binding.composePlaceholder.gone()
                    binding.favSearchInfo.setAdapter(ArrayAdapter(requireContext(), R.layout.favorite_auto_item, it.second.map { it.key }))
                    adapter.setData(it.second.toList())
                    binding.favSearchLayout.hint = resources.getQuantityString(R.plurals.numFavorites, it.first, it.first)
                    binding.favRv.smoothScrollToPosition(0)
                    if (it.second.isNotEmpty()) binding.emptyState.gone()
                    else binding.emptyState.visible()
                }
                .addTo(disposable)

            binding.composePlaceholder.setContent {
                MdcTheme {
                    LazyVerticalGrid(cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH)) {
                        items(10) { PlaceHolderCoverCard(placeHolder = logo2.notificationId) }
                    }
                }
            }

            binding.viewRecentList.setOnClickListener { (activity as? BaseMainActivity)?.goToScreen(BaseMainActivity.Screen.RECENT) }

            dbFire
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.groupBy { s -> s.source } }
                .subscribe { s ->
                    s.forEach { m ->
                        binding.sourceList.children.filterIsInstance<Chip>().find { it.text == m.key }?.text = "${m.key}: ${m.value.size}"
                    }
                }
                .addTo(disposable)
        }
    }

    private fun List<DbModel>.toGroup() = groupBy(DbModel::title)

    @SuppressLint("SetTextI18n")
    private fun uiSetup() {
        binding.favRv.layoutManager = AutoFitGridLayoutManager(requireContext(), 360).apply { orientation = GridLayoutManager.VERTICAL }
        binding.favRv.adapter = adapter
        binding.favRv.setItemViewCacheSize(20)
        binding.favRv.setHasFixedSize(true)

        binding.sourceList.addView(Chip(requireContext()).apply {
            text = "ALL"
            isCheckable = true
            isClickable = true
            isChecked = true
            setOnClickListener { binding.sourceList.children.filterIsInstance<Chip>().forEach { it.isChecked = true } }
        })

        sources.forEach {
            binding.sourceList.addView(Chip(requireContext()).apply {
                text = it.serviceName
                isCheckable = true
                isClickable = true
                isChecked = true
                setOnCheckedChangeListener { _, isChecked -> addOrRemoveSource(isChecked, it) }
                setOnLongClickListener {
                    binding.sourceList.clearCheck()
                    isChecked = true
                    true
                }
            })
        }

    }

    override fun onDestroy() {
        disposable.dispose()
        fireListener.unregister()
        super.onDestroy()
    }

    private fun addOrRemoveSource(isChecked: Boolean, sources: ApiService) {
        sourcesList = sourcesList?.apply { if (isChecked) add(sources) else remove(sources) }
    }

    private fun DragSwipeAdapter<Pair<String, List<DbModel>>, *>.setData(newList: List<Pair<String, List<DbModel>>>) {
        val diffCallback = object : DragSwipeDiffUtil<Pair<String, List<DbModel>>>(dataList, newList) {
            override fun areContentsTheSame(oldItem: Pair<String, List<DbModel>>, newItem: Pair<String, List<DbModel>>): Boolean =
                oldItem.second == newItem.second

            override fun areItemsTheSame(oldItem: Pair<String, List<DbModel>>, newItem: Pair<String, List<DbModel>>): Boolean =
                oldItem.second === newItem.second
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        dataList.clear()
        dataList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class FavoriteAdapter : DragSwipeAdapter<Pair<String, List<DbModel>>, FavoriteHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteHolder =
            FavoriteHolder(FavoriteItemBinding.inflate(requireContext().layoutInflater, parent, false))

        override fun FavoriteHolder.onBind(item: Pair<String, List<DbModel>>, position: Int) = bind(item.second, genericInfo)
    }

    class FavoriteHolder(private val binding: FavoriteItemBinding) : RecyclerView.ViewHolder(binding.root), KoinComponent {

        private val logo: MainLogo by inject()

        fun bind(info: List<DbModel>, genericInfo: GenericInfo) {
            binding.show = info.random()
            binding.root.toolTipText(info.random().title)
            Glide.with(itemView.context)
                .asBitmap()
                .load(info.random().imageUrl)
                .fallback(logo.logoId)
                .placeholder(logo.logoId)
                .error(logo.logoId)
                .fitCenter()
                .transform(RoundedCorners(15))
                .into(binding.galleryListCover)

            binding.root.setOnClickListener {
                if (info.size == 1) {
                    val item = info.firstOrNull()?.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                    binding.root.findNavController().navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                } else {
                    MaterialAlertDialogBuilder(itemView.context)
                        .setTitle(R.string.chooseASource)
                        .setItems(info.fastMap { "${it.source} - ${it.title}" }.toTypedArray()) { d, i ->
                            val item = info[i].let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                            binding.root.findNavController().navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                            d.dismiss()
                        }
                        .show()
                }
            }
            binding.executePendingBindings()
        }

    }

    @ExperimentalMaterialApi
    @Composable
    private fun FavoriteItem(info: Map.Entry<String, List<DbModel>>, navController: NavController, logoId: Int) {

        val context = LocalContext.current

        CoverCard(
            imageUrl = info.value.random().imageUrl,
            name = info.key,
            placeHolder = logoId
        ) {
            if (info.value.size == 1) {
                val item = info.value.firstOrNull()?.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                navController.navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
            } else {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.chooseASource)
                    .setItems(info.value.fastMap { "${it.source} - ${it.title}" }.toTypedArray()) { d, i ->
                        val item = info.value[i].let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                        navController.navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                        d.dismiss()
                    }
                    .show()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = FavoriteFragment()
    }

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    fun MainUi(favoriteItems: Flowable<List<DbModel>>, allSources: List<ApiService>) {

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
                                    confirmButton = {
                                        TextButton(onClick = { chooseSource = false }) { Text(stringResource(R.string.ok)) }
                                    }
                                )
                            }

                            CoverCard(
                                imageUrl = info.value.random().imageUrl,
                                name = info.key,
                                placeHolder = logo2.notificationId
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