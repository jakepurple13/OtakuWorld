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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.helpfulutils.colorFromTheme
import com.programmersbox.models.InfoModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.rxutils.invoke
import com.programmersbox.thirdpartyutils.*
import com.programmersbox.uiviews.databinding.DetailsFragmentBinding
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class DetailsFragment : Fragment() {

    companion object {
        fun newInstance() = DetailsFragment()
    }

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }

    private lateinit var binding: DetailsFragmentBinding

    private val args: DetailsFragmentArgs by navArgs()

    private val disposable = CompositeDisposable()

    private val adapter by lazy { ChapterAdapter(requireContext(), BaseMainActivity.genericInfo, dao) }

    private val isFavorite = BehaviorSubject.createDefault(false)

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
                binding.info = info
                binding.executePendingBindings()
                adapter.addItems(info.chapters)
                onInfoGet(info)

                Glide.with(binding.infoCover)
                    .load(info.imageUrl)
                    .override(360, 480)
                    /*.placeholder(R.drawable.manga_world_round_logo)
                    .error(R.drawable.manga_world_round_logo)
                    .fallback(R.drawable.manga_world_round_logo)*/
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
                                    adapter.swatchInfo = swatch
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
        binding.favoriteInfo.setTextColor(Color.WHITE)

        binding.favoriteItem.isEnabled = false
        binding.favoriteInfo.isEnabled = false
    }

    private fun onInfoGet(infoModel: InfoModel) {
        dao.getItemById(infoModel.url)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it > 0 }
            .subscribe(isFavorite::onNext)
            .addTo(disposable)

        binding.favoriteItem.isEnabled = true
        binding.favoriteInfo.isEnabled = true

        binding.favoriteItem.setOnClickListener {

            fun addItem(model: InfoModel) {
                val db = model.toDbModel(model.chapters.size)
                Completable.concatArray(
                    Completable.complete(),//FirebaseDb.insertShow(show.toShowModel()),
                    dao.insertFavorite(db).subscribeOn(Schedulers.io())
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { isFavorite(true) }
                    .addTo(disposable)
            }

            fun removeItem(model: InfoModel) {
                Completable.concatArray(
                    Completable.complete(),//FirebaseDb.removeShow(show.toShowModel()),
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
        val window = requireActivity().window
        ValueAnimator.ofArgb(window.statusBarColor, requireContext().colorFromTheme(R.attr.colorPrimaryVariant))
            .apply { addUpdateListener { window.statusBarColor = it.animatedValue as Int } }
            .start()
    }

}