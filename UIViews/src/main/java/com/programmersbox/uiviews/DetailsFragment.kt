package com.programmersbox.uiviews

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.dragswipe.get
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.helpfulutils.changeDrawableColor
import com.programmersbox.helpfulutils.colorFromTheme
import com.programmersbox.helpfulutils.gone
import com.programmersbox.helpfulutils.whatIfNotNull
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.databinding.DetailsFragmentBinding
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
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import my.nanihadesuka.compose.LazyColumnScrollbar
import org.koin.android.ext.android.inject

class DetailsFragment : Fragment() {

    companion object {
        fun newInstance() = DetailsFragment()
    }

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }

    private lateinit var binding: DetailsFragmentBinding

    private val args: DetailsFragmentArgs by navArgs()

    private val disposable = CompositeDisposable()

    private val adapter by lazy { ChapterAdapter(requireContext(), inject<GenericInfo>().value, dao, disposable) }

    private val genericInfo by inject<GenericInfo>()

    private val itemListener = FirebaseDb.FirebaseListener()
    private val chapterListener = FirebaseDb.FirebaseListener()

    private val logo: MainLogo by inject()
    private val logo2: NotificationLogo by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val COMPOSE_ONLY = true

    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NavigationUI.setupWithNavController(binding.collapsingBar, binding.toolbar, findNavController())

        binding.composeHeader.setContent { MdcTheme { PlaceHolderHeader() } }

        args.itemInfo
            ?.also { item ->
                binding.collapsingBar.title = item.title
                binding.toolbar.title = item.title
                binding.shareButton.setOnClickListener {
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, item.url)
                        putExtra(Intent.EXTRA_TITLE, item.title)
                    }, "Share ${item.title}"))
                }
            }
            ?.toInfoModel()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeBy { info ->
                if (COMPOSE_ONLY) {
                    binding.appBar.gone()
                    binding.chapterLayout.gone()
                    binding.shareButton.gone()
                }
                adapter.title = info.title
                adapter.itemUrl = info.url
                binding.info = info
                binding.executePendingBindings()
                adapter.addItems(info.chapters)
                if (!COMPOSE_ONLY) {
                    onInfoGet(info)

                    Glide.with(this)
                        .load(info.imageUrl)
                        .override(360, 480)
                        .placeholder(logo.logoId)
                        .error(logo.logoId)
                        .fallback(logo.logoId)
                        .transform(RoundedCorners(15))
                        .into<Drawable> {
                            resourceReady { image, _ ->
                                binding.swatch = image.getPalette().vibrantSwatch
                                    ?.let { SwatchInfo(it.rgb, it.titleTextColor, it.bodyTextColor) }
                                    .also { swatch ->

                                        adapter.swatchInfo = swatch

                                        swatch?.rgb?.let {
                                            binding.toolbar.setBackgroundColor(it)
                                            binding.shareButton.backgroundTintList = ColorStateList.valueOf(it)
                                            binding.collapsingBar.setContentScrimColor(it)
                                            requireActivity().window.statusBarColor = it
                                        }

                                        swatch?.titleColor?.let { binding.shareButton.setColorFilter(it) }

                                        FastScrollerBuilder(binding.infoChapterList)
                                            .useMd2Style()
                                            .whatIfNotNull(ContextCompat.getDrawable(requireContext(), R.drawable.afs_md2_thumb)) { drawable ->
                                                swatch?.bodyColor?.let { drawable.changeDrawableColor(it) }
                                                setThumbDrawable(drawable)
                                            }
                                            .build()
                                    }

                                binding.executePendingBindings()
                            }
                        }

                    binding.composeHeader.setContent {

                        val favoriteListener by combine(
                            itemListener.findItemByUrlFlow(info.url),
                            dao.containsItemFlow(info.url)
                        ) { f, d -> f || d }
                            .flowOn(Dispatchers.IO)
                            .flowWithLifecycle(lifecycle)
                            .collectAsState(initial = false)

                        val swatchInfo = remember { mutableStateOf<SwatchInfo?>(null) }

                        MdcTheme {
                            DetailsHeader(
                                model = info,
                                logo = painterResource(id = logo2.notificationId),
                                swatchInfo = swatchInfo,
                                isFavorite = favoriteListener
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
                    }
                }

                if (COMPOSE_ONLY) {
                    composeView(info)
                }

                binding.infoUrl.setOnClickListener {
                    requireContext().openInCustomChromeBrowser(info.url) { setShareState(CustomTabsIntent.SHARE_STATE_ON) }
                }

                dao.doesNotificationExist(info.url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { binding.toolbar.menu.findItem(R.id.saveForLater).isVisible = !it }
                    .addTo(disposable)

                binding.toolbar.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.markChaptersAs -> {
                            val checked = adapter.currentList
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.markAs)
                                .setMultiChoiceItems(
                                    adapter.dataList.fastMap(ChapterModel::name).toTypedArray(),
                                    BooleanArray(adapter.itemCount) { i -> checked.fastAny { it1 -> it1.url == adapter[i].url } }
                                ) { _, i, _ ->
                                    (binding.infoChapterList.findViewHolderForAdapterPosition(i) as? ChapterAdapter.ChapterHolder)
                                        ?.binding?.readChapter?.performClick()
                                }
                                .setPositiveButton(R.string.done) { d, _ -> d.dismiss() }
                                .show()
                        }
                        R.id.openBrowser -> {
                            requireContext().openInCustomChromeBrowser(info.url) { setShareState(CustomTabsIntent.SHARE_STATE_ON) }
                        }
                        R.id.saveForLater -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                dao.insertNotification(
                                    NotificationItem(
                                        id = info.hashCode(),
                                        url = info.url,
                                        summaryText = requireContext()
                                            .getString(R.string.hadAnUpdate, info.title, info.chapters.firstOrNull()?.name ?: ""),
                                        notiTitle = info.title,
                                        imageUrl = info.imageUrl,
                                        source = info.source.serviceName,
                                        contentTitle = info.title
                                    )
                                ).subscribe()
                            }
                        }
                        R.id.globalSearchByName -> findNavController().navigate(R.id.show_global_search, bundleOf("searchFor" to info.title))
                    }
                    true
                }

            }
            ?.addTo(disposable)

        binding.infoChapterList.adapter = adapter

        binding.infoUrl.transformationMethod = ChromeCustomTabTransformationMethod(requireContext()) {
            setShareState(CustomTabsIntent.SHARE_STATE_ON)
        }
        binding.infoUrl.movementMethod = LinkMovementMethod.getInstance()
        binding.infoUrl.paintFlags += Paint.UNDERLINE_TEXT_FLAG

    }

    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    private fun composeView(info: InfoModel) {
        val chaptersRead = Flowables.combineLatest(
            chapterListener.getAllEpisodesByShow(info.url),
            dao.getAllChapters(info.url).subscribeOn(Schedulers.io())
        ) { f, d -> (f + d).distinctBy { it.url } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        binding.fullComposeTest.setContent {
            val favoriteListener by combine(
                itemListener.findItemByUrlFlow(info.url),
                dao.containsItemFlow(info.url)
            ) { f, d -> f || d }
                .flowOn(Dispatchers.IO)
                .flowWithLifecycle(lifecycle)
                .collectAsState(initial = false)

            val chapters by chaptersRead.subscribeAsState(initial = emptyList())

            val isSaved by dao.doesNotificationExist(info.url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAsState(false)

            MdcTheme {

                val swatchInfo = remember { mutableStateOf<SwatchInfo?>(null) }

                val systemUiController = rememberSystemUiController()

                swatchInfo.value?.rgb?.toComposeColor()?.let { s ->
                    systemUiController.setStatusBarColor(
                        color = s,
                        darkIcons = s.luminance() > .5f
                    )
                }

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
                        val topBarColor = swatchInfo.value?.bodyColor?.toComposeColor()
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
                                    ) { Text(stringResource(id = R.string.fallback_menu_item_open_in_browser)) }

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
                                        ) { Text(stringResource(id = R.string.save_for_later)) }
                                    }

                                    DropdownMenuItem(
                                        onClick = {
                                            dropDownDismiss()
                                            findNavController().navigate(R.id.show_global_search, bundleOf("searchFor" to info.title))
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
                                ) { Icon(Icons.Default.Share, null, tint = topBarColor) }

                                IconButton(onClick = { showDropDown = true }) {
                                    Icon(Icons.Default.MoreVert, null, tint = topBarColor)
                                }
                            },
                            backgroundColor = swatchInfo.value?.rgb?.toComposeColor() ?: MaterialTheme.colors.primarySurface,
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
                    }
                ) { p ->

                    val header: @Composable () -> Unit = {
                        DetailsHeader(
                            model = info,
                            logo = painterResource(id = logo2.notificationId),
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
                            thumbSelectedColor = (swatchInfo.value?.bodyColor?.toComposeColor() ?: MaterialTheme.colors.primary)
                                .copy(alpha = .6f),
                        ) {
                            LazyColumn(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 5.dp),
                                state = listState
                            ) {
                                items(info.chapters) { c ->
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

        Card(
            onClick = {
                genericInfo.chapterOnClick(c, chapters, context)
                ChapterWatched(url = c.url, name = c.name, favoriteUrl = infoModel.url)
                    .let { Completable.mergeArray(FirebaseDb.insertEpisodeWatched(it), dao.insertChapter(it)) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { snackbar(R.string.addedChapterItem) }
                    .addTo(disposable)
            },
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth(),
            backgroundColor = swatchInfo.value?.rgb?.toComposeColor() ?: MaterialTheme.colors.surface
        ) {

            Column(modifier = Modifier.padding(16.dp)) {

                Row {
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
                            checkedColor = swatchInfo.value?.bodyColor?.toComposeColor() ?: MaterialTheme.colors.secondary,
                            uncheckedColor = swatchInfo.value?.bodyColor?.toComposeColor() ?: MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            checkmarkColor = swatchInfo.value?.rgb?.toComposeColor() ?: MaterialTheme.colors.surface
                        )
                    )

                    Text(
                        c.name,
                        style = MaterialTheme.typography.body1
                            .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it)) } ?: b },
                        modifier = Modifier.padding(start = 5.dp)
                    )

                }

                Text(
                    c.uploaded,
                    style = MaterialTheme.typography.subtitle2
                        .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it)) } ?: b },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(5.dp)
                )

                Row {

                    OutlinedButton(
                        onClick = {
                            genericInfo.chapterOnClick(c, chapters, context)
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
                        border = BorderStroke(1.dp, swatchInfo.value?.bodyColor?.toComposeColor() ?: Color.White)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Play",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tint = swatchInfo.value?.bodyColor?.toComposeColor()
                                    ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                            )
                            Text(
                                stringResource(R.string.read),
                                style = MaterialTheme.typography.button
                                    .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it)) } ?: b },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { genericInfo.downloadChapter(c, infoModel.title) },
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(horizontal = 5.dp),
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(1.dp, swatchInfo.value?.bodyColor?.toComposeColor() ?: Color.White)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.Download,
                                "Download",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tint = swatchInfo.value?.bodyColor?.toComposeColor()
                                    ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                            )
                            Text(
                                stringResource(R.string.download_chapter),
                                style = MaterialTheme.typography.button
                                    .let { b -> swatchInfo.value?.bodyColor?.let { b.copy(color = Color(it)) } ?: b },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                }

            }

        }

    }

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

        //var swatchInfo by remember { mutableStateOf<SwatchInfo?>(null) }

        var imagePopup by remember { mutableStateOf(false) }

        if (imagePopup) {

            AlertDialog(
                onDismissRequest = { imagePopup = false },
                title = { Text(model.title, modifier = Modifier.padding(5.dp)) },
                text = {
                    GlideImage(
                        imageModel = model.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.None,
                        requestBuilder = Glide.with(LocalView.current).asDrawable().transform(RoundedCorners(5)),
                        modifier = Modifier
                            .scaleRotateOffset()
                            .defaultMinSize(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                    )
                },
                confirmButton = { TextButton(onClick = { imagePopup = false }) { Text(stringResource(R.string.done)) } }
            )

        }

        var descriptionVisibility by remember { mutableStateOf(false) }

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
                                200//127
                            )
                            .toComposeColor()
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
                    modifier = Modifier.padding(start = 5.dp)
                ) {

                    LazyRow(
                        modifier = Modifier.padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(model.genres) {
                            CustomChip(
                                category = it,
                                textColor = swatchInfo.value?.rgb?.toComposeColor(),
                                backgroundColor = swatchInfo.value?.bodyColor?.toComposeColor()?.copy(1f),
                                modifier = Modifier.fadeInAnimation()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clickable { favoriteClick(isFavorite) }
                            .semantics(true) {}
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = swatchInfo.value?.rgb?.toComposeColor() ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            stringResource(if (isFavorite) R.string.removeFromFavorites else R.string.addToFavorites),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Text(
                        model.description,
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                            .clickable { descriptionVisibility = !descriptionVisibility },
                        overflow = TextOverflow.Ellipsis,
                        maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                        style = MaterialTheme.typography.body2,
                    )

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
                        painter = painterResource(id = R.drawable.ic_baseline_cloud_off_24),
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

    private fun onInfoGet(infoModel: InfoModel) {
        Flowables.combineLatest(
            chapterListener.getAllEpisodesByShow(infoModel.url),
            dao.getAllChapters(infoModel.url).subscribeOn(Schedulers.io())
        ) { f, d -> (f + d).distinctBy { it.url } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            //.distinct { it }
            .subscribe { adapter.update(it) { c, m -> c.url == m.url } }
            .addTo(disposable)
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