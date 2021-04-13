package com.programmersbox.uiviews

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.helpfulutils.changeDrawableColor
import com.programmersbox.helpfulutils.colorFromTheme
import com.programmersbox.helpfulutils.whatIfNotNull
import com.programmersbox.models.InfoModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.rxutils.invoke
import com.programmersbox.thirdpartyutils.*
import com.programmersbox.uiviews.databinding.DetailsFragmentBinding
import com.programmersbox.uiviews.utils.FirebaseDb
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class DetailsFragment : Fragment() {

    companion object {
        fun newInstance() = DetailsFragment()
    }

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }

    private lateinit var binding: DetailsFragmentBinding

    private val args: DetailsFragmentArgs by navArgs()

    private val disposable = CompositeDisposable()

    private val adapter by lazy { ChapterAdapter(requireContext(), BaseMainActivity.genericInfo, dao) }

    private val itemListener = FirebaseDb.FirebaseListener()
    private val chapterListener = FirebaseDb.FirebaseListener()

    private val isFavorite = BehaviorSubject.createDefault(false)

    private var swatchBarColor: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        args.itemInfo?.toInfoModel()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeBy { info ->
                adapter.title = info.title
                adapter.itemUrl = info.url
                binding.info = info
                binding.executePendingBindings()
                adapter.addItems(info.chapters)
                onInfoGet(info)

                Glide.with(binding.infoCover)
                    .load(info.imageUrl)
                    .override(360, 480)
                    .placeholder(OtakuApp.logo)
                    .error(OtakuApp.logo)
                    .fallback(OtakuApp.logo)
                    .transform(RoundedCorners(15))
                    .into<Drawable> {
                        resourceReady { image, _ ->
                            binding.infoCover.setImageDrawable(image)
                            binding.swatch = image.getPalette().vibrantSwatch
                                ?.let { SwatchInfo(it.rgb, it.titleTextColor, it.bodyTextColor) }
                                .also { swatch ->
                                    swatch?.rgb?.let { binding.infoLayout.setBackgroundColor(it) }
                                    swatch?.rgb?.let { binding.moreInfo.setBackgroundColor(it) }
                                    swatch?.titleColor?.let { binding.moreInfo.setTextColor(it) }
                                    swatch?.rgb?.let {
                                        binding.markChapters.strokeColor = ColorStateList.valueOf(it)
                                        binding.markChapters.setTextColor(it)
                                    }
                                    swatch?.rgb?.let {
                                        binding.shareButton.strokeColor = ColorStateList.valueOf(it)
                                        binding.shareButton.iconTint = ColorStateList.valueOf(it)
                                    }
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

            }
            ?.addTo(disposable)

        binding.infoChapterList.adapter = adapter

        binding.infoUrl.transformationMethod = ChromeCustomTabTransformationMethod(requireContext()) {
            //swatch?.rgb?.let { setToolbarColor(it) }
            setShareState(CustomTabsIntent.SHARE_STATE_ON)
        }
        binding.infoUrl.movementMethod = LinkMovementMethod.getInstance()

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
            dao.getItemById(infoModel.url)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.second > 0 || it.first }
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

    /*override fun onResume() {
        super.onResume()
        swatchBarColor?.let {
            requireActivity().window?.let { it1 ->
                ValueAnimator.ofArgb(it1.statusBarColor, it)
                    .apply { addUpdateListener { it1.statusBarColor = it.animatedValue as Int } }
                    .start()
            }
        }
    }*/

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