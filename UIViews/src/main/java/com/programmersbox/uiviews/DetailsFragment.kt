package com.programmersbox.uiviews

import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.navigation.fragment.navArgs
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.models.InfoModel
import com.programmersbox.rxutils.invoke
import com.programmersbox.thirdpartyutils.ChromeCustomTabTransformationMethod
import com.programmersbox.thirdpartyutils.changeTint
import com.programmersbox.thirdpartyutils.check
import com.programmersbox.uiviews.databinding.DetailsFragmentBinding
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DetailsFragment : Fragment() {

    companion object {
        fun newInstance() = DetailsFragment()
    }

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }

    private lateinit var binding: DetailsFragmentBinding

    private val args: DetailsFragmentArgs by navArgs()

    private val disposable = CompositeDisposable()

    private val adapter by lazy { ChapterAdapter(requireContext(), BaseMainActivity.genericInfo) }

    private val isFavorite = BehaviorSubject.createDefault(false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DetailsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //viewModel = ViewModelProvider(this).get(DetailsViewModel::class.java)
        //binding.info = args.itemInfo
        args.itemInfo?.toInfoModel()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribeBy {
                binding.info = it
                binding.executePendingBindings()
                adapter.addItems(it.chapters)
                onInfoGet(it)
            }
            ?.addTo(disposable)

        binding.infoChapterList.adapter = adapter

        binding.infoUrl.transformationMethod = ChromeCustomTabTransformationMethod(requireContext()) {
            //swatch?.rgb?.let { setToolbarColor(it) }
            setShareState(CustomTabsIntent.SHARE_STATE_ON)
        }
        binding.infoUrl.movementMethod = LinkMovementMethod.getInstance()

        isFavorite
            .subscribe { binding.favoriteItem.check(it) }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

}