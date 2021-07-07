package com.programmersbox.animeworldtv

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.programmersbox.anime_sources.Sources
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.models.ItemModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.*

class FavoriteFragment : BrowseSupportFragment() {

    private val mHandler = Handler()
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        prepareBackgroundManager()

        setupUIElements()

        loadRows()

        setupEventListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
        disposable.dispose()
        itemListener.unregister()
    }

    private fun prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(requireActivity().window)
        mDefaultBackground = ContextCompat.getDrawable(requireContext(), R.drawable.default_background)
        mMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        // over title
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(requireContext(), R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.search_opaque)
    }

    private val disposable = CompositeDisposable()
    private val itemDao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val itemListener = FirebaseDb.FirebaseListener()

    private fun loadRows() {
        /*val list = Sources.ANIMEKISA_SUBBED.getRecent().blockingGet()
            .map {
                MovieList.buildMovieInfo(
                    title = it.title,
                    description = it.description,
                    studio = it.source.serviceName,
                    videoUrl = it.url,
                    cardImageUrl = it.imageUrl,
                    backgroundImageUrl = it.imageUrl
                )
            }*/

        //MovieList.list

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()

        /*for (i in 0 until NUM_ROWS) {
            if (i != 0) {
                Collections.shuffle(list)
            }
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            for (j in 0 until NUM_COLS) {
                listRowAdapter.add(list[j % 5])
            }
            val header = HeaderItem(i.toLong(), MovieList.MOVIE_CATEGORY[i])
            rowsAdapter.add(ListRow(header, listRowAdapter))
        }*/

        Flowables.combineLatest(
            itemListener.getAllShowsFlowable(),
            itemDao.getAllFavorites()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        ) { fire, db -> (db + fire).groupBy(DbModel::url).map { it.value.maxByOrNull(DbModel::numChapters)!! } }
            //Yts.getRecent()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                it.mapNotNull {
                    try {
                        it.toItemModel(Sources.valueOf(it.source))
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            }
            .map { it.groupBy { it.source } }
            .subscribeBy {
                rowsAdapter.clear()
                it.entries.forEach {
                    val (t, u) = it
                    val listRowAdapter = ArrayObjectAdapter(cardPresenter)

                    listRowAdapter.addAll(0, u)

                    val header = HeaderItem(t?.hashCode()?.toLong() ?: 0L, t.toString())
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                }

                /*for (i in 0 until NUM_ROWS) {
                    if (i != 0) {
                        Collections.shuffle(it)
                    }
                    val listRowAdapter = ArrayObjectAdapter(cardPresenter)
                    for (j in 0 until NUM_COLS) {
                        listRowAdapter.add(it[j % 5])
                    }
                    val header = HeaderItem(i.toLong(), MovieList.MOVIE_CATEGORY[i])
                    rowsAdapter.add(ListRow(header, listRowAdapter))
                }*/
            }
            .addTo(disposable)

        adapter = rowsAdapter
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {

            if (item is ItemModel) {
                Log.d(TAG, "Item: $item")
                val intent = Intent(context!!, DetailsActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, item)

                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity!!,
                    (itemViewHolder.view as CustomImageCardView).mainImageView,
                    DetailsActivity.SHARED_ELEMENT_NAME
                )
                    .toBundle()
                startActivity(intent, bundle)
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            /*if (item is Movie) {
                mBackgroundUri = item.backgroundImageUrl
                startBackgroundTimer()
            }*/
        }
    }

    private fun updateBackground(uri: String?) {
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        Glide.with(requireContext())
            .load(uri)
            .centerCrop()
            .error(mDefaultBackground)
            .into<SimpleTarget<Drawable>>(
                object : SimpleTarget<Drawable>(width, height) {
                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        mBackgroundManager.drawable = drawable
                    }
                })
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    private inner class UpdateBackgroundTask : TimerTask() {

        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    private inner class GridItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(context!!, R.color.default_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return Presenter.ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            (viewHolder.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {}
    }

    companion object {
        private val TAG = "FavoritesFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
        private val NUM_ROWS = 6
        private val NUM_COLS = 15
    }
}