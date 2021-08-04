package com.programmersbox.uiviews

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.accompanist.placeholder.material.placeholder
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.dragswipe.get
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.helpfulutils.changeDrawableColor
import com.programmersbox.helpfulutils.colorFromTheme
import com.programmersbox.helpfulutils.whatIfNotNull
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.databinding.DetailsFragmentBinding
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
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
import me.zhanghai.android.fastscroll.FastScrollerBuilder
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

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NavigationUI.setupWithNavController(binding.collapsingBar, binding.toolbar, findNavController())

        binding.composeHeader.setContent { MdcTheme { PlaceHolderHeader(logo = logo2.notificationId) } }

        args.itemInfo
            ?.also {
                binding.collapsingBar.title = it.title
                binding.toolbar.title = it.title
            }
            ?.toInfoModel()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeBy { info ->
                adapter.title = info.title
                adapter.itemUrl = info.url
                binding.info = info
                binding.executePendingBindings()
                adapter.addItems(info.chapters)
                onInfoGet(info)

                binding.composeHeader.setContent {

                    var swatchInfo by remember { mutableStateOf<SwatchInfo?>(null) }

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

                                        swatchInfo = swatch
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

                    val favoriteListener by combine(
                        itemListener.findItemByUrlFlow(info.url),
                        dao.containsItemFlow(info.url)
                    ) { f, d -> f || d }
                        .flowOn(Dispatchers.IO)
                        .flowWithLifecycle(lifecycle)
                        .collectAsState(initial = false)

                    val logoRemember = remember { logo2.notificationId }

                    MdcTheme {
                        DetailsHeader(
                            model = info,
                            logo = logoRemember,
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

                /*binding.composeChapterList.setContent {

                    var swatchInfo by remember { mutableStateOf<SwatchInfo?>(null) }

                    LazyColumn {
                        items(info.chapters) {
                            ComposeChapterItem(
                                model = it,
                                watchedList = Flowables.combineLatest(
                                    chapterListener.getAllEpisodesByShow(info.url),
                                    dao.getAllChapters(info.url).subscribeOn(Schedulers.io())
                                ) { f, d -> (f + d).distinctBy { it.url } }
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeAsState(initial = emptyList()),
                                dao = dao,
                                disposable = CompositeDisposable(),
                                itemUrl = info.url,
                                info = genericInfo,
                                chapterList = info.chapters,
                                title = info.title,
                                swatchInfo = swatchInfo
                            )
                        }
                    }
                }*/

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
                                    adapter.dataList.map(ChapterModel::name).toTypedArray(),
                                    BooleanArray(adapter.itemCount) { i -> checked.any { it1 -> it1.url == adapter[i].url } }
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

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    @Composable
    private fun DetailsHeader(
        model: InfoModel,
        logo: Int,
        swatchInfo: SwatchInfo?,
        isFavorite: Boolean,
        favoriteClick: (Boolean) -> Unit
    ) {

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
                        requestBuilder = Glide.with(LocalView.current).asBitmap().transform(RoundedCorners(5)),
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
                requestBuilder = Glide.with(LocalView.current)
                    .asBitmap()
                    .placeholder(logo)
                    .error(logo)
                    .fallback(logo),
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
                                    swatchInfo?.rgb ?: Color.Transparent.toArgb(),
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
                        requestBuilder = Glide.with(LocalView.current)
                            .asBitmap()
                            //.override(360, 480)
                            .placeholder(logo)
                            .error(logo)
                            .fallback(logo)
                            .transform(RoundedCorners(5)),
                        error = BitmapFactory.decodeResource(resources, logo).asImageBitmap(),
                        placeHolder = BitmapFactory.decodeResource(resources, logo).asImageBitmap(),
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
                                textColor = swatchInfo?.rgb?.toComposeColor(),
                                backgroundColor = swatchInfo?.bodyColor?.toComposeColor()?.copy(1f),
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
                            tint = swatchInfo?.rgb?.toComposeColor() ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
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
    private fun PlaceHolderHeader(logo: Int) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
        ) {

            Row(
                modifier = Modifier
                    .padding(5.dp)
                    .animateContentSize()
            ) {

                Card(
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.padding(5.dp)
                ) {
                    Image(
                        painter = painterResource(id = logo),
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
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        style = MaterialTheme.typography.body2,
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

        binding.shareButton.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, infoModel.url)
                putExtra(Intent.EXTRA_TITLE, infoModel.title)
            }, "Share ${infoModel.title}"))
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