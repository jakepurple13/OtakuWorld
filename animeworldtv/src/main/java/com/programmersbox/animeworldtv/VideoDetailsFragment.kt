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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.MainLogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

/**
 * A wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
class VideoDetailsFragment : DetailsSupportFragment() {

    private var mSelectedMovie: ItemModel? = null
    private var mSelectedInfoModel: InfoModel? = null

    private val logo: MainLogo by inject()

    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var mPresenterSelector: ClassPresenterSelector
    private lateinit var mAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate DetailsFragment")
        super.onCreate(savedInstanceState)

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this)

        mSelectedMovie = requireActivity().intent.getSerializableExtra(DetailsActivity.MOVIE) as ItemModel

        lifecycleScope.launch {
            mSelectedMovie
                ?.toInfoModelFlow()
                ?.map { it.getOrNull() }
                ?.flowOn(Dispatchers.Main)
                ?.catch {
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                }
                ?.onEach {
                    if (it == null) {
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        mSelectedInfoModel = it
                        mPresenterSelector = ClassPresenterSelector()
                        mAdapter = ArrayObjectAdapter(mPresenterSelector)
                        setupDetailsOverviewRow(it)
                        setupDetailsOverviewRowPresenter(it)
                        setupRelatedMovieListRow(it)

                        lifecycleScope.launch {
                            combine(
                                chapterListener.getAllEpisodesByShowFlow(it.url),
                                itemDao.getAllChaptersFlow(it.url)
                            ) { f, d -> (f + d).distinctBy { it.url } }
                                .flowOn(Dispatchers.Main)
                                .onEach { l ->
                                    episodePresenter.watched.clear()
                                    episodePresenter.watched.addAll(l)
                                    rows.forEach { it.notifyChanges() }
                                }
                                .collect()
                        }

                        adapter = mAdapter
                        initializeBackground(it)
                        onItemViewClickedListener = ItemViewClickedListener()
                    }
                }
                ?.collect()
        }

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
    private val chapterListener = FirebaseDb.FirebaseListener()

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


        lifecycleScope.launch(Dispatchers.IO) {
            combine(
                itemListener.findItemByUrlFlow(movie!!.url),
                itemDao.containsItemFlow(movie.url)
            ) { f, d -> f || d }
                .flowOn(Dispatchers.Main)
                .collect {
                    actionAdapter.replace(
                        1,
                        Action(
                            5L,
                            resources.getString(if (it) R.string.removeFromFavorites else R.string.addToFavorites)
                        )//.also { it.icon = R.drawable.exo_ic_check }
                    )
                    isFavorite = it
                }
        }

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
                        lifecycleScope.launch {
                            itemDao.insertFavoriteFlow(db)
                            FirebaseDb.insertShowFlow(db).collect()
                        }
                    }

                    fun removeItem(model: InfoModel) {
                        val db = model.toDbModel(model.chapters.size)
                        lifecycleScope.launch {
                            itemDao.deleteFavoriteFlow(db)
                            FirebaseDb.removeShowFlow(db).collect()
                        }
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

    override fun onDestroy() {
        super.onDestroy()
        itemListener.unregister()
        chapterListener.unregister()
    }

    private val episodePresenter = EpisodePresenter()
    private val rows = mutableListOf<RefreshableArrayObjectAdapter>()

    private fun setupRelatedMovieListRow(model: InfoModel, watched: List<ChapterWatched> = emptyList()) {
        /*val subcategories = arrayOf(getString(R.string.related_movies))
        val list = MovieList.list

        Collections.shuffle(list)*/

        val chapters = model.chapters.reversed()//.map { it.copy(it.name.removePrefix(model.title)) }

        chapters.chunked(5).forEachIndexed { index, list ->
            val listRowAdapter = RefreshableArrayObjectAdapter(episodePresenter)
            listRowAdapter.addAll(0, list)
            rows.add(listRowAdapter)
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
        return (dp.toFloat() * density).roundToInt()
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
                if (!item.source.canPlay) {
                    Toast.makeText(
                        context,
                        requireContext().getString(R.string.source_no_stream, item.source.serviceName),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                ChapterWatched(url = item.url, name = item.name, favoriteUrl = mSelectedInfoModel?.url.orEmpty())
                    .let {
                        lifecycleScope.launch {
                            itemDao.insertChapterFlow(it)
                            FirebaseDb.insertEpisodeWatchedFlow(it).collect()
                        }
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

    class RefreshableArrayObjectAdapter(presenter: Presenter) : ArrayObjectAdapter(presenter) {
        override fun isImmediateNotifySupported(): Boolean = true
        fun replaceAll(list: Collection<Any>) {
            clear()
            addAll(0, list)
            notifyChanged()
        }

        fun notifyChanges() = notifyChanged()
    }

}