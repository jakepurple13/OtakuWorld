package com.programmersbox.animeworldtv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {

    private var mSelectedMovie: ItemModel? = null

    private val logo: MainLogo by inject()

    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var mPresenterSelector: ClassPresenterSelector
    private lateinit var mAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this)

        mSelectedMovie = requireActivity().intent.getSerializableExtra(DetailsActivity.MOVIE) as ItemModel

        mSelectedMovie
            ?.toInfoModel()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.doOnError {
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
            }
            ?.subscribeBy {
                mPresenterSelector = ClassPresenterSelector()
                mAdapter = ArrayObjectAdapter(mPresenterSelector)
                setupDetailsOverviewRow(it)
                setupDetailsOverviewRowPresenter(it)
                setupRelatedMovieListRow(it)
                adapter = mAdapter
                initializeBackground(it)
                onItemViewClickedListener = ItemViewClickedListener()
            }
            ?.addTo(disposable)

        /*if (mSelectedMovie != null) {
            mPresenterSelector = ClassPresenterSelector()
            mAdapter = ArrayObjectAdapter(mPresenterSelector)
            setupDetailsOverviewRow()
            setupDetailsOverviewRowPresenter()
            //setupRelatedMovieListRow()
            adapter = mAdapter
            initializeBackground(mSelectedMovie)
            onItemViewClickedListener = ItemViewClickedListener()
        } else {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }*/
    }

    private fun initializeBackground(movie: InfoModel?) {
        mDetailsBackground.enableParallax()
        Glide.with(requireContext())
            .asBitmap()
            .centerCrop()
            .error(R.drawable.default_background)
            .load(movie?.imageUrl)
            .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(
                    bitmap: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    mDetailsBackground.coverBitmap = bitmap
                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                }
            })
    }

    private val itemDao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val itemListener = FirebaseDb.FirebaseListener()

    private fun setupDetailsOverviewRow(movie: InfoModel?) {
        Log.d(TAG, "doInBackground: " + mSelectedMovie?.toString())
        val row = DetailsOverviewRow(mSelectedMovie)
        row.imageDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.default_background)
        val width = convertDpToPixel(requireContext(), DETAIL_THUMB_WIDTH)
        val height = convertDpToPixel(requireContext(), DETAIL_THUMB_HEIGHT)
        Glide.with(requireContext())
            .load(movie?.imageUrl)
            .centerCrop()
            .placeholder(logo.logoId)
            .error(R.drawable.default_background)
            .into<SimpleTarget<Drawable>>(object : SimpleTarget<Drawable>(width, height) {
                override fun onResourceReady(
                    drawable: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    Log.d(TAG, "details overview card image url ready: $drawable")
                    row.imageDrawable = drawable
                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                }
            })

        val actionAdapter = ArrayObjectAdapter()

        /*movie?.chapters?.reversed()?.forEach {
            actionAdapter.add(
                Action(
                    it.hashCode().toLong(),
                    it.name,
                    it.uploaded
                )
            )
        }*/

        actionAdapter.add(
            Action(
                4L,
                "Source",
                movie?.source?.serviceName.orEmpty(),
            )
        )


        Flowables.combineLatest(
            itemListener.findItemByUrl(movie!!.url),
            itemDao.containsItem(movie!!.url)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.second || it.first }
            .subscribe {
                actionAdapter.replace(
                    1,
                    Action(
                        5L,
                        resources.getString(if (it) R.string.removeFromFavorites else R.string.addToFavorites)
                    )//.also { it.icon = R.drawable.exo_ic_check }
                )
                isFavorite = it
            }
            .addTo(disposable)

        actionAdapter.add(
            Action(
                5L,
                resources.getString(R.string.addToFavorites)
            )
        )

        //actionAdapter.replace()

        /*actionAdapter.add(
            Action(
                ACTION_WATCH_TRAILER,
                resources.getString(R.string.watch_trailer_1),
                resources.getString(R.string.watch_trailer_2)
            )
        )
        actionAdapter.add(
            Action(
                ACTION_RENT,
                resources.getString(R.string.rent_1),
                resources.getString(R.string.rent_2)
            )
        )
        actionAdapter.add(
            Action(
                ACTION_BUY,
                resources.getString(R.string.buy_1),
                resources.getString(R.string.buy_2)
            )
        )*/
        row.actionsAdapter = actionAdapter

        mAdapter.add(row)
    }

    private var isFavorite = false

    private fun setupDetailsOverviewRowPresenter(movie: InfoModel?) {
        // Set detail background.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter(movie))
        detailsPresenter.backgroundColor = ContextCompat.getColor(requireContext(), R.color.detail_background)

        // Hook up transition element.
        val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper()
        sharedElementHelper.setSharedElementEnterTransition(
            activity, DetailsActivity.SHARED_ELEMENT_NAME
        )
        detailsPresenter.setListener(sharedElementHelper)
        detailsPresenter.isParticipatingEntranceTransition = true

        detailsPresenter.onActionClickedListener = OnActionClickedListener { action ->

            /*movie?.chapters?.find { it.hashCode().toLong() == action.id }
                ?.let {
                    val intent = Intent(requireContext(), PlaybackActivity::class.java)
                    intent.putExtra(DetailsActivity.MOVIE, it)
                    startActivity(intent)
                }*/

            when (action.id) {
                5L -> {

                    fun addItem(model: InfoModel) {
                        val db = model.toDbModel(model.chapters.size)
                        Completable.concatArray(
                            FirebaseDb.insertShow(db),
                            itemDao.insertFavorite(db).subscribeOn(Schedulers.io())
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
                            itemDao.deleteFavorite(model.toDbModel()).subscribeOn(Schedulers.io())
                        )
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe()
                            .addTo(disposable)
                    }

                    movie?.let { (if (isFavorite) ::removeItem else ::addItem)(it) }

                    /*movie?.toDbModel(movie.chapters.size)?.let {
                        if(isFavorite) itemDao.deleteFavorite(it) else itemDao.insertFavorite(it)
                    }
                        ?.subscribeOn(Schedulers.io())
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribe()
                        ?.addTo(disposable)*/
                }
                ACTION_WATCH_TRAILER -> {

                    /*
                            ?.subscribeOn(Schedulers.io())
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.flatMap {
                                println(it)
                                it.chapters.firstOrNull()?.getChapterInfo()
                                    ?.subscribeOn(Schedulers.io())
                                    ?.observeOn(AndroidSchedulers.mainThread())
                            }
                            ?.subscribeBy {
                                mSelectedMovie?.videoUrl = it.firstOrNull()?.link

                                val intent = Intent(requireContext(), PlaybackActivity::class.java)
                                intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie)
                                startActivity(intent)

                            }
                            ?.addTo(disposable)*/

                }
                else -> {
                    //Toast.makeText(requireContext(), action.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
        mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private val disposable = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        itemListener.unregister()
    }

    private fun setupRelatedMovieListRow(model: InfoModel) {
        /*val subcategories = arrayOf(getString(R.string.related_movies))
        val list = MovieList.list

        Collections.shuffle(list)*/

        val chapters = model.chapters.reversed()//.map { it.copy(it.name.removePrefix(model.title)) }

        chapters.chunked(5).forEachIndexed { index, list ->
            val listRowAdapter = ArrayObjectAdapter(EpisodePresenter())
            listRowAdapter.addAll(0, list)
            val header = HeaderItem(index.toLong(), "Episodes ${(index * 5) + 1} - ${index * 5 + list.size}")
            mAdapter.add(ListRow(header, listRowAdapter))
        }
        /*for (j in 0 until 5) {
            listRowAdapter.add(chapters[j % 5])
        }*/


        /*val listRowAdapter = ArrayObjectAdapter(EpisodePresenter())
        listRowAdapter.addAll(0, model.chapters.reversed())

        val header = HeaderItem(0, "Episodes")
        mAdapter.add(ListRow(header, listRowAdapter))*/
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
    }

    private fun convertDpToPixel(context: Context, dp: Int): Int {
        val density = context.applicationContext.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder?,
            item: Any?,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            /*if (item is Movie) {
                Log.d(TAG, "Item: $item")
                val intent = Intent(context!!, DetailsActivity::class.java)
                intent.putExtra(resources.getString(R.string.movie), mSelectedMovie)

                val bundle =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity!!,
                        (itemViewHolder?.view as ImageCardView).mainImageView,
                        DetailsActivity.SHARED_ELEMENT_NAME
                    )
                        .toBundle()
                startActivity(intent, bundle)
            } else {
                Toast.makeText(requireContext(), item.toString(), Toast.LENGTH_SHORT).show()
            }*/

            if (item is ChapterModel) {
                if ((item.source as? ShowApi)?.canStream == false) {
                    Toast.makeText(
                        context,
                        requireContext().getString(R.string.source_no_stream, item.source.serviceName),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                val intent = Intent(requireContext(), PlaybackActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, item)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), item.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private val TAG = "VideoDetailsFragment"

        private val ACTION_WATCH_TRAILER = 1L
        private val ACTION_RENT = 2L
        private val ACTION_BUY = 3L

        private val DETAIL_THUMB_WIDTH = 274
        private val DETAIL_THUMB_HEIGHT = 274

        private val NUM_COLS = 10
    }
}