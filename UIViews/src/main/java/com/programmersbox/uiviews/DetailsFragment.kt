package com.programmersbox.uiviews

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.noowenz.showmoreless.ShowMoreLess
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
import com.programmersbox.rxutils.invoke
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.thirdpartyutils.changeTint
import com.programmersbox.thirdpartyutils.check
import com.programmersbox.thirdpartyutils.getPalette
import com.programmersbox.thirdpartyutils.into
import com.programmersbox.uiviews.databinding.DetailsFragmentBinding
import com.programmersbox.uiviews.utils.ChromeCustomTabTransformationMethod
import com.programmersbox.uiviews.utils.openInCustomChromeBrowser
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

    private val adapter by lazy { ChapterAdapter(requireContext(), inject<GenericInfo>().value, dao) }

    private val itemListener = FirebaseDb.FirebaseListener()
    private val chapterListener = FirebaseDb.FirebaseListener()

    private val logo: MainLogo by inject()

    private val isFavorite = BehaviorSubject.createDefault(false)

    private var swatchBarColor: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NavigationUI.setupWithNavController(binding.collapsingBar, binding.toolbar, findNavController())
        args.itemInfo
            ?.also {
                binding.collapsingBar.title = it.title
                binding.infoDescription.text = it.description
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

                Glide.with(binding.bigInfoCover)
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

        isFavorite
            .subscribe {
                binding.favoriteItem.check(it)
                binding.favoriteInfo.text = getText(if (it) R.string.removeFromFavorites else R.string.addToFavorites)
            }
            .addTo(disposable)

        binding.favoriteItem.changeTint(Color.WHITE)

        binding.favoriteItem.isEnabled = false
        binding.favoriteInfo.isEnabled = false

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

        binding.favoriteItem.isEnabled = true
        binding.favoriteInfo.isEnabled = true

        binding.favoriteItem.setOnClickListener {

            fun addItem(model: InfoModel) {
                val db = model.toDbModel(model.chapters.size)
                Completable.concatArray(
                    FirebaseDb.insertShow(db),
                    dao.insertFavorite(db).subscribeOn(Schedulers.io())
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { isFavorite(true) }
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
                    .subscribe { isFavorite(false) }
                    .addTo(disposable)
            }

            (if (isFavorite.value!!) ::removeItem else ::addItem)(infoModel)

        }

        binding.favoriteInfo.setOnClickListener { binding.favoriteItem.performClick() }

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