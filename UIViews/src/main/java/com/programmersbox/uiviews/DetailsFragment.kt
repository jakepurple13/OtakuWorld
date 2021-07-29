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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.dragswipe.get
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
import com.programmersbox.thirdpartyutils.getPalette
import com.programmersbox.thirdpartyutils.into
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
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.Dispatchers
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

    private val isFavorite = BehaviorSubject.createDefault(false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @ExperimentalMaterialApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NavigationUI.setupWithNavController(binding.collapsingBar, binding.toolbar, findNavController())
        args.itemInfo
            ?.also {
                binding.collapsingBar.title = it.title
                binding.toolbar.title = it.title
            }
            ?.toInfoModel()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeBy { info ->
                binding.shimmer.shimmerLayout.stopShimmer()
                binding.shimmer.shimmerLayout.gone()
                adapter.title = info.title
                adapter.itemUrl = info.url
                binding.info = info
                binding.executePendingBindings()
                adapter.addItems(info.chapters)
                onInfoGet(info)

                /* Glide.with(binding.bigInfoCover)
                     .load(info.imageUrl)
                     .override(360, 480)
                     .placeholder(logo.logoId)
                     .error(logo.logoId)
                     .fallback(logo.logoId)
                     .transform(RoundedCorners(15))
                     .into<Drawable> {
                         resourceReady { image, _ ->
                             binding.bigInfoCover.setImageDrawable(image)
                             binding.infoCover.setImageDrawable(image)
                             binding.swatch = image.getPalette().vibrantSwatch
                                 ?.let { SwatchInfo(it.rgb, it.titleTextColor, it.bodyTextColor) }
                                 .also { swatch ->
                                     swatch?.rgb?.let {
                                         binding.infoHeader.setBackgroundColor(
                                             ColorUtils.setAlphaComponent(
                                                 ColorUtils.blendARGB(
                                                     requireContext().colorFromTheme(R.attr.colorSurface),
                                                     it,
                                                     0.25f
                                                 ),
                                                 127
                                             )
                                         )

                                         binding.collapsingBar.setContentScrimColor(it)
                                     }

                                     //swatchInfo = swatch

                                     ShowMoreLess.Builder(requireContext())
                                         .expandAnimation(true)
                                         .showMoreLabel(getString(R.string.showMore))
                                         .showLessLabel(getString(R.string.showLess))
                                         .labelBold(true)
                                         .textLengthAndLengthType(2, ShowMoreLess.TYPE_LINE)
                                         .whatIfNotNull(swatch?.rgb) {
                                             showLessLabelColor(it)
                                             showMoreLabelColor(it)
                                         }
                                         .textClickable(textClickableInExpand = true, textClickableInCollapse = true)
                                         .labelUnderLine(true)
                                         .build()
                                         .addShowMoreLess(binding.infoDescription, binding.infoDescription.text, false)

                                     swatch?.rgb?.let { binding.toolbar.setBackgroundColor(it) }
                                     swatch?.rgb?.let { binding.infoLayout.setBackgroundColor(it) }
                                     swatch?.rgb?.let { binding.shareButton.backgroundTintList = ColorStateList.valueOf(it) }
                                     swatch?.titleColor?.let { binding.shareButton.setColorFilter(it) }
                                     binding.favoriteItem.changeTint(swatch?.rgb ?: Color.WHITE)
                                     swatch?.rgb?.let { requireActivity().window.statusBarColor = it }
                                     swatchBarColor = swatch?.rgb
                                     adapter.swatchInfo = swatch

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
                     }*/

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

                    val favoriteListener by Flowables.combineLatest(
                        itemListener.findItemByUrl(info.url),
                        dao.containsItem(info.url)
                    )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map { it.second || it.first }
                        .subscribeAsState(initial = false)

                    MdcTheme {
                        DetailsHeader(
                            model = info,
                            logo = logo,
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

    @ExperimentalMaterialApi
    @Composable
    private fun DetailsHeader(
        model: InfoModel,
        logo: MainLogo,
        swatchInfo: SwatchInfo?,
        isFavorite: Boolean,
        favoriteClick: (Boolean) -> Unit
    ) {

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
                    .placeholder(logo.logoId)
                    .error(logo.logoId)
                    .fallback(logo.logoId),
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
                            .placeholder(logo.logoId)
                            .error(logo.logoId)
                            .fallback(logo.logoId)
                            .transform(RoundedCorners(5)),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                        failure = {
                            Image(
                                bitmap = BitmapFactory.decodeResource(resources, logo.logoId).asImageBitmap(),
                                contentDescription = model.title,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            )
                        }
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
                            .padding(vertical = 5.dp)
                            .fillMaxWidth()
                    ) {

                        /*val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.heart))
                        val p by animateLottieCompositionAsState(composition = composition)

                        *//*
                        fun LottieAnimationView.changeTint(@ColorInt newColor: Int) =
        addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) { PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_ATOP) }
                         *//*

                        LottieAnimation(
                            composition = composition,
                            progress = animateFloatAsState(targetValue = ),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )*/

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

    private fun onInfoGet(infoModel: InfoModel) {

        Flowables.combineLatest(
            itemListener.findItemByUrl(infoModel.url),
            dao.containsItem(infoModel.url)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.second || it.first }
            .subscribe(isFavorite::onNext)
            .addTo(disposable)

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