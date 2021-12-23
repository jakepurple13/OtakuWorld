package com.programmersbox.uiviews

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.*
import com.programmersbox.helpfulutils.colorFromTheme
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.palette.BitmapPalette
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import my.nanihadesuka.compose.LazyColumnScrollbar
import org.koin.android.ext.android.inject
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.contentColorFor as m3ContentColorFor

class DetailsFragment : Fragment() {

    companion object {
        fun newInstance() = DetailsFragment()
    }

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val historyDao by lazy { HistoryDatabase.getInstance(requireContext()).historyDao() }

    private val args: DetailsFragmentArgs by navArgs()

    private val disposable = CompositeDisposable()

    private val genericInfo by inject<GenericInfo>()

    private val logo: NotificationLogo by inject()

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))

        args.itemInfo
            ?.also { info ->
                currentDetailsUrl = info.url
                setContent {

                    val localContext = LocalContext.current
                    val details: DetailViewModel = viewModel(factory = factoryCreate { DetailViewModel(info, localContext) })

                    M3MaterialTheme(currentColorScheme) {
                        if (details.info == null) {
                            Scaffold(
                                topBar = {
                                    SmallTopAppBar(
                                        modifier = Modifier.zIndex(2f),
                                        title = { Text(info.title) },
                                        navigationIcon = {
                                            IconButton(onClick = { findNavController().popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                                        },
                                        actions = {
                                            IconButton(
                                                onClick = {
                                                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, info.url)
                                                        putExtra(Intent.EXTRA_TITLE, info.title)
                                                    }, getString(R.string.share_item, info.title)))
                                                }
                                            ) { Icon(Icons.Default.Share, null) }

                                            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
                                        },
                                    )
                                }
                            ) { PlaceHolderHeader() }
                        } else if (details.info != null) {

                            val windowSize = requireActivity().rememberWindowSizeClass()
                            val orientation = LocalConfiguration.current.orientation

                            val isSaved by dao.doesNotificationExist(info.url)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeAsState(false)

                            val shareChapter by localContext.shareChapter.collectAsState(initial = true)
                            val swatchInfo = remember { mutableStateOf<SwatchInfo?>(null) }

                            val systemUiController = rememberSystemUiController()
                            val statusBarColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()

                            LaunchedEffect(statusBarColor) {
                                statusBarColor?.value?.let { s ->
                                    systemUiController.setStatusBarColor(color = s, darkIcons = s.luminance() > .5f)
                                }
                            }

                            if (
                                windowSize == WindowSize.Medium ||
                                windowSize == WindowSize.Expanded ||
                                orientation == Configuration.ORIENTATION_LANDSCAPE
                            ) {
                                DetailsViewLandscape(
                                    details.info!!,
                                    details.chapters,
                                    details.favoriteListener,
                                    isSaved,
                                    shareChapter,
                                    swatchInfo
                                )
                            } else {
                                DetailsView(
                                    details.info!!,
                                    details.chapters,
                                    details.favoriteListener,
                                    isSaved,
                                    shareChapter,
                                    swatchInfo
                                )
                            }

                        }
                    }
                }
            }
    }

    class DetailViewModel(
        itemModel: ItemModel? = null,
        context: Context,
    ) : ViewModel() {

        var info: InfoModel? by mutableStateOf(null)

        private val disposable = CompositeDisposable()
        private val dao = ItemDatabase.getInstance(context).itemDao()

        private val itemListener = FirebaseDb.FirebaseListener()
        private val chapterListener = FirebaseDb.FirebaseListener()

        var favoriteListener by mutableStateOf(false)
        var chapters: List<ChapterWatched> by mutableStateOf(emptyList())

        private val itemSub = itemModel
            ?.toInfoModel()
            ?.doOnError { context.showErrorToast() }
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeBy {
                info = it
                setup(it)
            }

        private fun setup(info: InfoModel) {

            viewModelScope.launch(Dispatchers.IO) {
                combine(
                    itemListener.findItemByUrlFlow(info.url),
                    dao.containsItemFlow(info.url)
                ) { f, d -> f || d }
                    .collect { favoriteListener = it }
            }

            Flowables.combineLatest(
                chapterListener.getAllEpisodesByShow(info.url),
                dao.getAllChapters(info.url).subscribeOn(Schedulers.io())
            ) { f, d -> (f + d).distinctBy { it.url } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { chapters = it }
                .addTo(disposable)

        }

        override fun onCleared() {
            super.onCleared()
            itemSub?.dispose()
            disposable.dispose()
            itemListener.unregister()
            chapterListener.unregister()
        }
    }

    @Composable
    private fun Color.animate() = animateColorAsState(this)

    @ExperimentalComposeUiApi
    @ExperimentalMaterial3Api
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    @Composable
    private fun DetailsViewLandscape(
        info: InfoModel,
        chapters: List<ChapterWatched>,
        favoriteListener: Boolean,
        isSaved: Boolean,
        shareChapter: Boolean,
        swatchInfo: MutableState<SwatchInfo?>
    ) {

        var reverseChapters by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val scaffoldState = rememberBottomSheetScaffoldState()

        BackHandler(scaffoldState.bottomSheetState.isExpanded && findNavController().graph.id == currentScreen.value) {
            scope.launch {
                try {
                    scaffoldState.bottomSheetState.collapse()
                } catch (e: Exception) {
                    findNavController().popBackStack()
                }
            }
        }

        fun showSnackBar(text: Int, duration: SnackbarDuration = SnackbarDuration.Short) {
            scope.launch {
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                scaffoldState.snackbarHostState.showSnackbar(getString(text), null, duration)
            }
        }

        val topBarColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
            ?: M3MaterialTheme.colorScheme.onSurface

        BottomSheetScaffold(
            backgroundColor = Color.Transparent,
            sheetContent = {
                Scaffold(
                    topBar = {
                        SmallTopAppBar(
                            title = { Text(stringResource(id = R.string.markAs), color = topBarColor) },
                            colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                            ),
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { scaffoldState.bottomSheetState.collapse() } }) {
                                    Icon(Icons.Default.Close, null, tint = topBarColor)
                                }
                            }
                        )
                    },
                ) { p ->
                    LazyColumn(
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(info.chapters) { c ->
                            fun markAs(b: Boolean) {
                                ChapterWatched(url = c.url, name = c.name, favoriteUrl = info.url)
                                    .let {
                                        Completable.mergeArray(
                                            if (b) FirebaseDb.insertEpisodeWatched(it) else FirebaseDb.removeEpisodeWatched(it),
                                            if (b) dao.insertChapter(it) else dao.deleteChapter(it)
                                        )
                                    }
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe {}
                                    .addTo(disposable)
                            }

                            Surface(
                                onClick = { markAs(!chapters.fastAny { it.url == c.url }) },
                                shape = RoundedCornerShape(0.dp),
                                tonalElevation = 5.dp,
                                modifier = Modifier.fillMaxWidth(),
                                indication = rememberRipple(),
                                color = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                            ) {
                                ListItem(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    text = {
                                        Text(
                                            c.name,
                                            color = swatchInfo.value
                                                ?.bodyColor
                                                ?.toComposeColor()
                                                ?.animate()?.value ?: M3MaterialTheme.typography.titleMedium.color
                                        )
                                    },
                                    icon = {
                                        androidx.compose.material3.Checkbox(
                                            checked = chapters.fastAny { it.url == c.url },
                                            onCheckedChange = { b -> markAs(b) },
                                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                                checkedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                                    ?: M3MaterialTheme.colorScheme.secondary,
                                                uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                                    ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                checkmarkColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value
                                                    ?: M3MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            sheetPeekHeight = 0.dp,
            sheetGesturesEnabled = false,
            scaffoldState = scaffoldState,
            topBar = {
                SmallTopAppBar(
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        titleContentColor = topBarColor,
                        containerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.zIndex(2f),
                    title = { Text(info.title) },
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = topBarColor)
                        }
                    },
                    actions = {
                        var showDropDown by remember { mutableStateOf(false) }

                        val dropDownDismiss = { showDropDown = false }

                        MdcTheme {
                            DropdownMenu(
                                expanded = showDropDown,
                                onDismissRequest = dropDownDismiss,
                            ) {

                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        scope.launch { scaffoldState.bottomSheetState.expand() }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(stringResource(id = R.string.markAs))
                                }

                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        requireContext().openInCustomChromeBrowser(info.url) { setShareState(CustomTabsIntent.SHARE_STATE_ON) }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.OpenInBrowser,
                                        null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(stringResource(id = R.string.fallback_menu_item_open_in_browser))
                                }

                                if (!isSaved) {
                                    DropdownMenuItem(
                                        onClick = {
                                            dropDownDismiss()
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                dao.insertNotification(
                                                    NotificationItem(
                                                        id = info.hashCode(),
                                                        url = info.url,
                                                        summaryText = requireContext()
                                                            .getString(
                                                                R.string.hadAnUpdate,
                                                                info.title,
                                                                info.chapters.firstOrNull()?.name.orEmpty()
                                                            ),
                                                        notiTitle = info.title,
                                                        imageUrl = info.imageUrl,
                                                        source = info.source.serviceName,
                                                        contentTitle = info.title
                                                    )
                                                ).subscribe()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Save,
                                            null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(stringResource(id = R.string.save_for_later))
                                    }
                                }

                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        findNavController().navigate(GlobalNavDirections.showGlobalSearch(info.title))
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(stringResource(id = R.string.global_search_by_name))
                                }

                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        reverseChapters = !reverseChapters
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Sort,
                                        null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(stringResource(id = R.string.reverseOrder))
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, info.url)
                                    putExtra(Intent.EXTRA_TITLE, info.title)
                                }, getString(R.string.share_item, info.title)))
                            }
                        ) { Icon(Icons.Default.Share, null, tint = topBarColor) }

                        genericInfo.DetailActions(infoModel = info, tint = topBarColor)

                        IconButton(onClick = { showDropDown = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = topBarColor)
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(it) { data ->
                    val background = swatchInfo.value?.rgb?.toComposeColor() ?: SnackbarDefaults.backgroundColor
                    val font = swatchInfo.value?.titleColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.surface
                    Snackbar(
                        elevation = 15.dp,
                        backgroundColor = Color(ColorUtils.blendARGB(background.toArgb(), M3MaterialTheme.colorScheme.onSurface.toArgb(), .25f)),
                        contentColor = font,
                        snackbarData = data
                    )
                }
            },
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            swatchInfo.value?.rgb
                                ?.toComposeColor()
                                ?.animate()?.value ?: M3MaterialTheme.colorScheme.background,
                            M3MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) { p ->

            Row(
                modifier = Modifier.padding(p)
            ) {

                DetailsHeader(
                    modifier = Modifier.weight(1f),
                    model = info,
                    logo = painterResource(id = logo.notificationId),
                    isFavorite = favoriteListener,
                    swatchInfo = swatchInfo
                ) { b ->
                    fun addItem(model: InfoModel) {
                        val db = model.toDbModel(model.chapters.size)
                        Completable.concatArray(
                            FirebaseDb.insertShow(db),
                            dao.insertFavorite(db).subscribeOn(Schedulers.io())
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()
                            .addTo(disposable)
                    }

                    fun removeItem(model: InfoModel) {
                        val db = model.toDbModel(model.chapters.size)
                        Completable.concatArray(
                            FirebaseDb.removeShow(db),
                            dao.deleteFavorite(model.toDbModel()).subscribeOn(Schedulers.io())
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()
                            .addTo(disposable)
                    }

                    (if (b) ::removeItem else ::addItem)(info)
                }

                val listState = rememberLazyListState()

                var descriptionVisibility by remember { mutableStateOf(false) }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(vertical = 5.dp),
                    state = listState
                ) {

                    if (info.description.isNotEmpty()) {
                        item {
                            Text(
                                info.description,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = rememberRipple()
                                    ) { descriptionVisibility = !descriptionVisibility }
                                    .padding(horizontal = 5.dp)
                                    //.fillMaxWidth()
                                    .animateContentSize(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                                style = M3MaterialTheme.typography.bodyMedium,
                                color = M3MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    items(info.chapters.let { if (reverseChapters) it.reversed() else it }) { c ->
                        ChapterItem(
                            infoModel = info,
                            c = c,
                            read = chapters,
                            chapters = info.chapters,
                            swatchInfo = swatchInfo,
                            shareChapter = shareChapter,
                            snackbar = ::showSnackBar
                        )
                    }
                }
            }
        }
    }

    @ExperimentalComposeUiApi
    @ExperimentalMaterial3Api
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    @Composable
    private fun DetailsView(
        info: InfoModel,
        chapters: List<ChapterWatched>,
        favoriteListener: Boolean,
        isSaved: Boolean,
        shareChapter: Boolean,
        swatchInfo: MutableState<SwatchInfo?>
    ) {

        var reverseChapters by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val scaffoldState = rememberBottomSheetScaffoldState()

        BackHandler(scaffoldState.bottomSheetState.isExpanded && findNavController().graph.id == currentScreen.value) {
            scope.launch {
                try {
                    scaffoldState.bottomSheetState.collapse()
                } catch (e: Exception) {
                    findNavController().popBackStack()
                }
            }
        }

        fun showSnackBar(text: Int, duration: SnackbarDuration = SnackbarDuration.Short) {
            scope.launch {
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                scaffoldState.snackbarHostState.showSnackbar(getString(text), null, duration)
            }
        }

        val topBarColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
            ?: M3MaterialTheme.colorScheme.onSurface

        val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

        BottomSheetScaffold(
            backgroundColor = Color.Transparent,
            sheetContent = {
                Scaffold(
                    topBar = {
                        SmallTopAppBar(
                            title = { Text(stringResource(id = R.string.markAs), color = topBarColor) },
                            colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                            ),
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { scaffoldState.bottomSheetState.collapse() } }) {
                                    Icon(Icons.Default.Close, null, tint = topBarColor)
                                }
                            }
                        )
                    },
                ) { p ->
                    LazyColumn(
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(info.chapters) { c ->
                            fun markAs(b: Boolean) {
                                ChapterWatched(url = c.url, name = c.name, favoriteUrl = info.url)
                                    .let {
                                        Completable.mergeArray(
                                            if (b) FirebaseDb.insertEpisodeWatched(it) else FirebaseDb.removeEpisodeWatched(it),
                                            if (b) dao.insertChapter(it) else dao.deleteChapter(it)
                                        )
                                    }
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe {}
                                    .addTo(disposable)
                            }

                            Surface(
                                onClick = { markAs(!chapters.fastAny { it.url == c.url }) },
                                shape = RoundedCornerShape(0.dp),
                                tonalElevation = 5.dp,
                                modifier = Modifier.fillMaxWidth(),
                                indication = rememberRipple(),
                                color = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                            ) {
                                ListItem(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    text = {
                                        Text(
                                            c.name,
                                            color = swatchInfo.value
                                                ?.bodyColor
                                                ?.toComposeColor()
                                                ?.animate()?.value ?: M3MaterialTheme.typography.titleMedium.color
                                        )
                                    },
                                    icon = {
                                        androidx.compose.material3.Checkbox(
                                            checked = chapters.fastAny { it.url == c.url },
                                            onCheckedChange = { b -> markAs(b) },
                                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                                checkedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                                    ?: M3MaterialTheme.colorScheme.secondary,
                                                uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                                    ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                checkmarkColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value
                                                    ?: M3MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            sheetPeekHeight = 0.dp,
            sheetGesturesEnabled = false,
            scaffoldState = scaffoldState,
            topBar = {
                SmallTopAppBar(
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        titleContentColor = topBarColor,
                        containerColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.zIndex(2f),
                    scrollBehavior = scrollBehavior,
                    title = { Text(info.title) },
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = topBarColor)
                        }
                    },
                    actions = {
                        var showDropDown by remember { mutableStateOf(false) }

                        val dropDownDismiss = { showDropDown = false }

                        MdcTheme {
                            DropdownMenu(
                                expanded = showDropDown,
                                onDismissRequest = dropDownDismiss,
                            ) {

                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        scope.launch { scaffoldState.bottomSheetState.expand() }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(stringResource(id = R.string.markAs))
                                }

                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        requireContext().openInCustomChromeBrowser(info.url) { setShareState(CustomTabsIntent.SHARE_STATE_ON) }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.OpenInBrowser,
                                        null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(stringResource(id = R.string.fallback_menu_item_open_in_browser))
                                }

                                if (!isSaved) {
                                    DropdownMenuItem(
                                        onClick = {
                                            dropDownDismiss()
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                dao.insertNotification(
                                                    NotificationItem(
                                                        id = info.hashCode(),
                                                        url = info.url,
                                                        summaryText = requireContext()
                                                            .getString(
                                                                R.string.hadAnUpdate,
                                                                info.title,
                                                                info.chapters.firstOrNull()?.name.orEmpty()
                                                            ),
                                                        notiTitle = info.title,
                                                        imageUrl = info.imageUrl,
                                                        source = info.source.serviceName,
                                                        contentTitle = info.title
                                                    )
                                                ).subscribe()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Save,
                                            null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(stringResource(id = R.string.save_for_later))
                                    }
                                }

                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        findNavController().navigate(GlobalNavDirections.showGlobalSearch(info.title))
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(stringResource(id = R.string.global_search_by_name))
                                }

                                DropdownMenuItem(
                                    onClick = {
                                        dropDownDismiss()
                                        reverseChapters = !reverseChapters
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Sort,
                                        null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(stringResource(id = R.string.reverseOrder))
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, info.url)
                                    putExtra(Intent.EXTRA_TITLE, info.title)
                                }, getString(R.string.share_item, info.title)))
                            }
                        ) { Icon(Icons.Default.Share, null, tint = topBarColor) }

                        genericInfo.DetailActions(infoModel = info, tint = topBarColor)

                        IconButton(onClick = { showDropDown = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = topBarColor)
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(it) { data ->
                    val background = swatchInfo.value?.rgb?.toComposeColor() ?: SnackbarDefaults.backgroundColor
                    val font = swatchInfo.value?.titleColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.surface
                    Snackbar(
                        elevation = 15.dp,
                        backgroundColor = Color(ColorUtils.blendARGB(background.toArgb(), M3MaterialTheme.colorScheme.onSurface.toArgb(), .25f)),
                        contentColor = font,
                        snackbarData = data
                    )
                }
            },
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            swatchInfo.value?.rgb
                                ?.toComposeColor()
                                ?.animate()?.value ?: M3MaterialTheme.colorScheme.background,
                            M3MaterialTheme.colorScheme.background
                        )
                    )
                )
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { p ->

            val header: @Composable () -> Unit = {
                DetailsHeader(
                    model = info,
                    logo = painterResource(id = logo.notificationId),
                    isFavorite = favoriteListener,
                    swatchInfo = swatchInfo
                ) { b ->
                    fun addItem(model: InfoModel) {
                        val db = model.toDbModel(model.chapters.size)
                        Completable.concatArray(
                            FirebaseDb.insertShow(db),
                            dao.insertFavorite(db).subscribeOn(Schedulers.io())
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()
                            .addTo(disposable)
                    }

                    fun removeItem(model: InfoModel) {
                        val db = model.toDbModel(model.chapters.size)
                        Completable.concatArray(
                            FirebaseDb.removeShow(db),
                            dao.deleteFavorite(model.toDbModel()).subscribeOn(Schedulers.io())
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()
                            .addTo(disposable)
                    }

                    (if (b) ::removeItem else ::addItem)(info)
                }
            }

            val state = rememberCollapsingToolbarScaffoldState()

            CollapsingToolbarScaffold(
                modifier = Modifier.padding(p),
                state = state,
                scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
                toolbar = { header() }
            ) {
                val listState = rememberLazyListState()

                LazyColumnScrollbar(
                    thickness = 8.dp,
                    padding = 2.dp,
                    listState = listState,
                    thumbColor = swatchInfo.value?.bodyColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.primary,
                    thumbSelectedColor = (swatchInfo.value?.bodyColor?.toComposeColor() ?: M3MaterialTheme.colorScheme.primary).copy(alpha = .6f),
                ) {
                    var descriptionVisibility by remember { mutableStateOf(false) }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 5.dp),
                        state = listState
                    ) {

                        if (info.description.isNotEmpty()) {
                            item {
                                Text(
                                    info.description,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = rememberRipple()
                                        ) { descriptionVisibility = !descriptionVisibility }
                                        .padding(horizontal = 5.dp)
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                                    style = M3MaterialTheme.typography.bodyMedium,
                                    color = M3MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        items(info.chapters.let { if (reverseChapters) it.reversed() else it }) { c ->
                            ChapterItem(
                                infoModel = info,
                                c = c,
                                read = chapters,
                                chapters = info.chapters,
                                swatchInfo = swatchInfo,
                                shareChapter = shareChapter,
                                snackbar = ::showSnackBar
                            )
                        }
                    }
                }
            }
        }
    }

    @ExperimentalMaterialApi
    @Composable
    private fun ChapterItem(
        infoModel: InfoModel,
        c: ChapterModel,
        read: List<ChapterWatched>,
        chapters: List<ChapterModel>,
        swatchInfo: MutableState<SwatchInfo?>,
        shareChapter: Boolean,
        snackbar: (Int) -> Unit
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        fun insertRecent() {
            scope.launch(Dispatchers.IO) {
                historyDao.insertRecentlyViewed(
                    RecentModel(
                        title = infoModel.title,
                        url = infoModel.url,
                        imageUrl = infoModel.imageUrl,
                        description = infoModel.description,
                        source = infoModel.source.serviceName,
                        timestamp = System.currentTimeMillis()
                    )
                )
                historyDao.removeOldData()
            }
        }

        fun markAs(b: Boolean) {
            ChapterWatched(url = c.url, name = c.name, favoriteUrl = infoModel.url)
                .let {
                    Completable.mergeArray(
                        if (b) FirebaseDb.insertEpisodeWatched(it) else FirebaseDb.removeEpisodeWatched(it),
                        if (b) dao.insertChapter(it) else dao.deleteChapter(it)
                    )
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { snackbar(if (b) R.string.addedChapterItem else R.string.removedChapterItem) }
                .addTo(disposable)
        }

        Surface(
            onClick = { markAs(!read.fastAny { it.url == c.url }) },
            shape = RoundedCornerShape(0.dp),
            indication = rememberRipple(),
            modifier = Modifier.fillMaxWidth(),
            color = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface,
            tonalElevation = 5.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                if (shareChapter) {
                    ConstraintLayout(
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth()
                    ) {
                        val (checkbox, text, share) = createRefs()

                        androidx.compose.material3.Checkbox(
                            checked = read.fastAny { it.url == c.url },
                            onCheckedChange = { b -> markAs(b) },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: M3MaterialTheme.colorScheme.secondary,
                                uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                checkmarkColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.constrainAs(checkbox) {
                                start.linkTo(parent.start)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                            }
                        )

                        Text(
                            c.name,
                            style = M3MaterialTheme.typography.bodyLarge
                                .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                            modifier = Modifier
                                .padding(start = 5.dp)
                                .constrainAs(text) {
                                    start.linkTo(checkbox.end)
                                    end.linkTo(share.start)
                                    top.linkTo(parent.top)
                                    bottom.linkTo(parent.bottom)
                                    width = Dimension.fillToConstraints
                                }
                        )

                        IconButton(
                            modifier = Modifier
                                .padding(5.dp)
                                .constrainAs(share) {
                                    end.linkTo(parent.end)
                                    top.linkTo(parent.top)
                                    bottom.linkTo(parent.bottom)
                                },
                            onClick = {
                                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, c.url)
                                    putExtra(Intent.EXTRA_TITLE, c.name)
                                }, getString(R.string.share_item, c.name)))
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                null,
                                tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = read.fastAny { it.url == c.url },
                            onCheckedChange = { b -> markAs(b) },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: M3MaterialTheme.colorScheme.secondary,
                                uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                    ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                checkmarkColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: M3MaterialTheme.colorScheme.surface
                            )
                        )

                        Text(
                            c.name,
                            style = M3MaterialTheme.typography.bodyLarge
                                .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                            modifier = Modifier.padding(start = 5.dp)
                        )
                    }
                }

                Text(
                    c.uploaded,
                    style = M3MaterialTheme.typography.titleSmall
                        .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(5.dp)
                )

                Row {
                    if (infoModel.source.canPlay) {
                        OutlinedButton(
                            onClick = {
                                genericInfo.chapterOnClick(c, chapters, infoModel, context)
                                insertRecent()
                                if (!read.fastAny { it.url == c.url }) markAs(true)
                            },
                            modifier = Modifier
                                .weight(1f, true)
                                .padding(horizontal = 5.dp),
                            //colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                            border = BorderStroke(1.dp, swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current)
                        ) {
                            Column {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    "Play",
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                        ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current)
                                )
                                Text(
                                    stringResource(R.string.read),
                                    style = M3MaterialTheme.typography.labelLarge
                                        .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }

                    if (infoModel.source.canDownload) {
                        OutlinedButton(
                            onClick = {
                                genericInfo.downloadChapter(c, chapters, infoModel, context)
                                insertRecent()
                                if (!read.fastAny { it.url == c.url }) markAs(true)
                            },
                            modifier = Modifier
                                .weight(1f, true)
                                .padding(horizontal = 5.dp),
                            //colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                            border = BorderStroke(1.dp, swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current)
                        ) {
                            Column {
                                Icon(
                                    Icons.Default.Download,
                                    "Download",
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                        ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current)
                                )
                                Text(
                                    stringResource(R.string.download_chapter),
                                    style = M3MaterialTheme.typography.labelLarge
                                        .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @ExperimentalComposeUiApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    @Composable
    private fun DetailsHeader(
        modifier: Modifier = Modifier,
        model: InfoModel,
        logo: Any?,
        isFavorite: Boolean,
        swatchInfo: MutableState<SwatchInfo?>,
        favoriteClick: (Boolean) -> Unit
    ) {

        var imagePopup by remember { mutableStateOf(false) }

        if (imagePopup) {

            AlertDialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { imagePopup = false },
                title = { Text(model.title, modifier = Modifier.padding(5.dp)) },
                text = {
                    GlideImage(
                        imageModel = model.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .scaleRotateOffsetReset()
                            .defaultMinSize(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    )
                },
                confirmButton = { TextButton(onClick = { imagePopup = false }) { Text(stringResource(R.string.done)) } }
            )

        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
                .then(modifier)
        ) {

            GlideImage(
                imageModel = model.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        ColorUtils
                            .setAlphaComponent(
                                ColorUtils.blendARGB(
                                    M3MaterialTheme.colorScheme.surface.toArgb(),
                                    swatchInfo.value?.rgb ?: Color.Transparent.toArgb(),
                                    0.25f
                                ),
                                200
                            )
                            .toComposeColor()
                            .animate().value
                    )
            )

            Row(
                modifier = Modifier
                    .padding(5.dp)
                    .animateContentSize()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(5.dp)
                ) {
                    GlideImage(
                        imageModel = model.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        error = logo,
                        placeHolder = logo,
                        bitmapPalette = BitmapPalette { p ->
                            swatchInfo.value = p.vibrantSwatch?.let { s -> SwatchInfo(s.rgb, s.titleTextColor, s.bodyTextColor) }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .combinedClickable(
                                onClick = {},
                                onDoubleClick = { imagePopup = true }
                            )
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        model.source.serviceName,
                        style = M3MaterialTheme.typography.labelSmall,
                        color = M3MaterialTheme.colorScheme.onSurface
                    )

                    var descriptionVisibility by remember { mutableStateOf(false) }

                    Text(
                        model.title,
                        style = M3MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple()
                            ) { descriptionVisibility = !descriptionVisibility },
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                        color = M3MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(model.genres) {
                            CustomChip(
                                category = it,
                                textColor = (swatchInfo.value?.rgb?.toComposeColor() ?: M3MaterialTheme.colorScheme.onSurface).animate().value,
                                backgroundColor = (swatchInfo.value?.bodyColor?.toComposeColor()?.copy(1f) ?: M3MaterialTheme.colorScheme.surface)
                                    .animate().value,
                                modifier = Modifier.fadeInAnimation()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple()
                            ) { favoriteClick(isFavorite) }
                            .semantics(true) {}
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value
                                ?: M3MaterialTheme.colorScheme.onSurface.copy(alpha = LocalContentAlpha.current),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            stringResource(if (isFavorite) R.string.removeFromFavorites else R.string.addToFavorites),
                            style = M3MaterialTheme.typography.headlineSmall,
                            fontSize = 20.sp,
                            modifier = Modifier.align(Alignment.CenterVertically),
                            color = M3MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        stringResource(R.string.chapter_count, model.chapters.size),
                        style = M3MaterialTheme.typography.bodyMedium,
                        color = M3MaterialTheme.colorScheme.onSurface
                    )

                    /*if(model.alternativeNames.isNotEmpty()) {
                        Text(
                            stringResource(R.string.alternateNames, model.alternativeNames.joinToString(", ")),
                            maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { descriptionVisibility = !descriptionVisibility }
                        )
                    }*/

                    /*
                    var descriptionVisibility by remember { mutableStateOf(false) }
                    Text(
                        model.description,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { descriptionVisibility = !descriptionVisibility },
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                        style = MaterialTheme.typography.body2,
                    )*/

                }

            }
        }
    }

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    @Composable
    private fun PlaceHolderHeader() {

        val placeholderColor = m3ContentColorFor(backgroundColor = M3MaterialTheme.colorScheme.surface)
            .copy(0.1f)
            .compositeOver(M3MaterialTheme.colorScheme.surface)

        Box(modifier = Modifier.fillMaxSize()) {

            Row(modifier = Modifier.padding(5.dp)) {

                Card(
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.padding(5.dp)
                ) {
                    Image(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .placeholder(true, color = placeholderColor)
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 5.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .placeholder(true, color = placeholderColor)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) { Text("") }

                    Row(
                        modifier = Modifier
                            .placeholder(true, color = placeholderColor)
                            .semantics(true) {}
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                    ) {

                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            stringResource(R.string.addToFavorites),
                            style = M3MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Text(
                        "Otaku".repeat(50),
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .placeholder(true, color = placeholderColor),
                        maxLines = 2
                    )

                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        val window = requireActivity().window
        ValueAnimator.ofArgb(window.statusBarColor, requireContext().colorFromTheme(R.attr.colorPrimaryVariant))
            .apply { addUpdateListener { window.statusBarColor = it.animatedValue as Int } }
            .start()
    }

}