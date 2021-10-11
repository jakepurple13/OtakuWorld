package com.programmersbox.uiviews

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.*
import com.programmersbox.helpfulutils.colorFromTheme
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState
import my.nanihadesuka.compose.LazyColumnScrollbar
import org.koin.android.ext.android.inject

class DetailsFragment : Fragment() {

    companion object {
        fun newInstance() = DetailsFragment()
    }

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val historyDao by lazy { HistoryDatabase.getInstance(requireContext()).historyDao() }

    private val args: DetailsFragmentArgs by navArgs()

    private val disposable = CompositeDisposable()

    private val genericInfo by inject<GenericInfo>()

    private val itemListener = FirebaseDb.FirebaseListener()
    private val chapterListener = FirebaseDb.FirebaseListener()

    private val logo: NotificationLogo by inject()

    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))

        args.itemInfo
            ?.also { info ->
                setContent {
                    MdcTheme {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    modifier = Modifier.zIndex(2f),
                                    title = { Text(info.title) },
                                    navigationIcon = {
                                        IconButton(onClick = { findNavController().popBackStack() }) {
                                            Icon(Icons.Default.ArrowBack, null)
                                        }
                                    },
                                    actions = {
                                        var showDropDown by remember { mutableStateOf(false) }

                                        val dropDownDismiss = { showDropDown = false }

                                        DropdownMenu(
                                            expanded = showDropDown,
                                            onDismissRequest = dropDownDismiss,
                                        ) {

                                            DropdownMenuItem(
                                                onClick = {
                                                    dropDownDismiss()
                                                    requireContext().openInCustomChromeBrowser(info.url) { setShareState(CustomTabsIntent.SHARE_STATE_ON) }
                                                }
                                            ) { Text(stringResource(id = R.string.fallback_menu_item_open_in_browser)) }

                                            DropdownMenuItem(
                                                onClick = {
                                                    dropDownDismiss()
                                                    findNavController().navigate(GlobalNavDirections.showGlobalSearch(info.title))
                                                }
                                            ) { Text(stringResource(id = R.string.global_search_by_name)) }
                                        }

                                        IconButton(
                                            onClick = {
                                                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, info.url)
                                                    putExtra(Intent.EXTRA_TITLE, info.title)
                                                }, "Share ${info.title}"))
                                            }
                                        ) { Icon(Icons.Default.Share, null) }

                                        IconButton(onClick = { showDropDown = true }) { Icon(Icons.Default.MoreVert, null) }
                                    },
                                )
                            }
                        ) { PlaceHolderHeader() }
                    }
                }
            }
            ?.toInfoModel()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeBy { info -> setContent { MdcTheme { DetailsView(info) } } }
            ?.addTo(disposable)
    }

    @Composable
    private fun Color.animate() = animateColorAsState(this)

    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    @Composable
    private fun DetailsView(info: InfoModel) {
        val favoriteListener by combine(
            itemListener.findItemByUrlFlow(info.url),
            dao.containsItemFlow(info.url)
        ) { f, d -> f || d }
            .flowOn(Dispatchers.IO)
            .flowWithLifecycle(lifecycle)
            .collectAsState(initial = false)

        val chapters by Flowables.combineLatest(
            chapterListener.getAllEpisodesByShow(info.url),
            dao.getAllChapters(info.url).subscribeOn(Schedulers.io())
        ) { f, d -> (f + d).distinctBy { it.url } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(initial = emptyList())

        val isSaved by dao.doesNotificationExist(info.url)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(false)

        val swatchInfo = remember { mutableStateOf<SwatchInfo?>(null) }

        val systemUiController = rememberSystemUiController()

        swatchInfo.value?.rgb?.toComposeColor()
            ?.animate()?.value
            ?.let { s ->
                systemUiController.setStatusBarColor(
                    color = s,
                    darkIcons = s.luminance() > .5f
                )
            }

        var reverseChapters by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()

        fun showSnackBar(text: Int, duration: SnackbarDuration = SnackbarDuration.Short) {
            scope.launch {
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                scaffoldState.snackbarHostState.showSnackbar(getString(text), null, duration)
            }
        }

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                val topBarColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                    ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)

                TopAppBar(
                    modifier = Modifier.zIndex(2f),
                    title = { Text(info.title, color = topBarColor) },
                    navigationIcon = {
                        IconButton(onClick = { findNavController().popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = topBarColor)
                        }
                    },
                    actions = {
                        var showDropDown by remember { mutableStateOf(false) }

                        val dropDownDismiss = { showDropDown = false }

                        DropdownMenu(
                            expanded = showDropDown,
                            onDismissRequest = dropDownDismiss,
                        ) {

                            //TODO: Don't forget to add the mark as dialog

                            /*DropdownMenuItem(
                                onClick = {
                                    dropDownDismiss()

                                }
                            ) { Text(stringResource(id = R.string.markAs)) }*/

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
                                                            info.chapters.firstOrNull()?.name ?: ""
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

                        IconButton(
                            onClick = {
                                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, info.url)
                                    putExtra(Intent.EXTRA_TITLE, info.title)
                                }, "Share ${info.title}"))
                            }
                        ) { Icon(Icons.Default.Share, null, tint = topBarColor) }

                        IconButton(onClick = { showDropDown = true }) {
                            Icon(Icons.Default.MoreVert, null, tint = topBarColor)
                        }
                    },
                    backgroundColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.primarySurface,
                )
            },
            snackbarHost = {
                SnackbarHost(it) { data ->
                    val background = swatchInfo.value?.rgb?.toComposeColor() ?: SnackbarDefaults.backgroundColor
                    val font = swatchInfo.value?.titleColor?.toComposeColor() ?: MaterialTheme.colors.surface
                    Snackbar(
                        elevation = 15.dp,
                        backgroundColor = Color(ColorUtils.blendARGB(background.toArgb(), MaterialTheme.colors.onSurface.toArgb(), .25f)),
                        contentColor = font,
                        snackbarData = data
                    )
                }
            },
            backgroundColor = Color.Transparent,
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.background,
                            MaterialTheme.colors.background
                        )
                    )
                )
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
                    thumbColor = swatchInfo.value?.bodyColor?.toComposeColor() ?: MaterialTheme.colors.primary,
                    thumbSelectedColor = (swatchInfo.value?.bodyColor?.toComposeColor() ?: MaterialTheme.colors.primary).copy(alpha = .6f),
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
                                        .clickable { descriptionVisibility = !descriptionVisibility }
                                        .padding(horizontal = 5.dp)
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = if (descriptionVisibility) Int.MAX_VALUE else 3,
                                    style = MaterialTheme.typography.body2
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

        Card(
            onClick = {
                genericInfo.chapterOnClick(c, chapters, infoModel, context)
                insertRecent()
                ChapterWatched(url = c.url, name = c.name, favoriteUrl = infoModel.url)
                    .let { Completable.mergeArray(FirebaseDb.insertEpisodeWatched(it), dao.insertChapter(it)) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { snackbar(R.string.addedChapterItem) }
                    .addTo(disposable)
            },
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.clickable {
                        val b = read.fastAny { it.url == c.url }
                        ChapterWatched(url = c.url, name = c.name, favoriteUrl = infoModel.url)
                            .let {
                                Completable.mergeArray(
                                    if (!b) FirebaseDb.insertEpisodeWatched(it) else FirebaseDb.removeEpisodeWatched(it),
                                    if (!b) dao.insertChapter(it) else dao.deleteChapter(it)
                                )
                            }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { snackbar(if (!b) R.string.addedChapterItem else R.string.removedChapterItem) }
                            .addTo(disposable)
                    }
                ) {
                    Checkbox(
                        checked = read.fastAny { it.url == c.url },
                        onCheckedChange = { b ->
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
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.secondary,
                            uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                ?: MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            checkmarkColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.surface
                        )
                    )

                    Text(
                        c.name,
                        style = MaterialTheme.typography.body1
                            .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                        modifier = Modifier.padding(start = 5.dp)
                    )

                }

                Text(
                    c.uploaded,
                    style = MaterialTheme.typography.subtitle2
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
                                ChapterWatched(url = c.url, name = c.name, favoriteUrl = infoModel.url)
                                    .let { Completable.mergeArray(FirebaseDb.insertEpisodeWatched(it), dao.insertChapter(it)) }
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe { snackbar(R.string.addedChapterItem) }
                                    .addTo(disposable)
                            },
                            modifier = Modifier
                                .weight(1f, true)
                                .padding(horizontal = 5.dp),
                            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                            border = BorderStroke(1.dp, swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current)
                        ) {
                            Column {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    "Play",
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                        ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                                )
                                Text(
                                    stringResource(R.string.read),
                                    style = MaterialTheme.typography.button
                                        .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it).animate().value) } ?: b },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }

                    if (infoModel.source.canDownload) {
                        OutlinedButton(
                            onClick = { genericInfo.downloadChapter(c, infoModel.title) },
                            modifier = Modifier
                                .weight(1f, true)
                                .padding(horizontal = 5.dp),
                            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                            border = BorderStroke(1.dp, swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: LocalContentColor.current)
                        ) {
                            Column {
                                Icon(
                                    Icons.Default.Download,
                                    "Download",
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value
                                        ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                                )
                                Text(
                                    stringResource(R.string.download_chapter),
                                    style = MaterialTheme.typography.button
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

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    @Composable
    private fun DetailsHeader(
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
                        requestBuilder = Glide.with(LocalView.current).asDrawable().transform(RoundedCorners(5)),
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
                                    MaterialTheme.colors.primarySurface.toArgb(),
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

                Card(
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.padding(5.dp)
                ) {
                    GlideImage(
                        imageModel = model.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        requestBuilder = Glide.with(LocalView.current).asDrawable().transform(RoundedCorners(5)),
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
                    modifier = Modifier.padding(start = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    Text(
                        model.source.serviceName,
                        style = MaterialTheme.typography.overline
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(model.genres) {
                            CustomChip(
                                category = it,
                                textColor = (swatchInfo.value?.rgb?.toComposeColor() ?: MaterialTheme.colors.onSurface).animate().value,
                                backgroundColor = (swatchInfo.value?.bodyColor?.toComposeColor()?.copy(1f) ?: MaterialTheme.colors.surface)
                                    .animate().value,
                                modifier = Modifier.fadeInAnimation()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clickable { favoriteClick(isFavorite) }
                            .semantics(true) {}
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value
                                ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            stringResource(if (isFavorite) R.string.removeFromFavorites else R.string.addToFavorites),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Text(
                        stringResource(R.string.chapter_count, model.chapters.size),
                        style = MaterialTheme.typography.body2
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
                            .placeholder(true)
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 5.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .placeholder(true)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) { Text("") }

                    Row(
                        modifier = Modifier
                            .placeholder(true)
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
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Text(
                        "Otaku".repeat(50),
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .placeholder(true),
                        maxLines = 2
                    )

                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        itemListener.unregister()
        chapterListener.unregister()
        val window = requireActivity().window
        ValueAnimator.ofArgb(window.statusBarColor, requireContext().colorFromTheme(R.attr.colorPrimaryVariant))
            .apply { addUpdateListener { window.statusBarColor = it.animatedValue as Int } }
            .start()
    }

}