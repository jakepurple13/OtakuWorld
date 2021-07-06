package com.programmersbox.animeworldtv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.leanback.app.SearchFragment
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import com.programmersbox.anime_sources.anime.WcoSubbed
import com.programmersbox.models.ItemModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CustomSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val handler = Handler()
    private val delayedLoad = SearchRunnable()
    private val searchList = mutableListOf<ItemModel>()
    private val disposable = CompositeDisposable()

    inner class SearchRunnable : Runnable {

        private var searchQuery = ""

        fun setSearchQuery(query: String) {
            searchQuery = query
        }

        override fun run() {

            searchList.filter { it.title.contains(searchQuery, true) }
                .groupBy { it.title.firstOrNull().toString() }
                .entries.forEach {
                    val (t, u) = it
                    val listRowAdapter = ArrayObjectAdapter(CardPresenter())

                    listRowAdapter.addAll(0, u)

                    val header = HeaderItem(t.hashCode().toLong(), t)

                    rowsAdapter.add(ListRow(header, listRowAdapter))
                }
        }

    }

    override fun getResultsAdapter(): ObjectAdapter = rowsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener(ItemViewClickedListener())
        WcoSubbed.getList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { searchList.addAll(it) }
            .addTo(disposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        rowsAdapter.clear()
        if (!TextUtils.isEmpty(newQuery)) {
            delayedLoad.setSearchQuery(newQuery)
            handler.removeCallbacks(delayedLoad)
            handler.postDelayed(delayedLoad, SEARCH_DELAY_MS)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        rowsAdapter.clear()
        if (!TextUtils.isEmpty(query)) {
            delayedLoad.setSearchQuery(query)
            handler.removeCallbacks(delayedLoad)
            handler.postDelayed(delayedLoad, SEARCH_DELAY_MS)
        }
        return true
    }

    companion object {
        private val SEARCH_DELAY_MS = 300L
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is ItemModel) {
                val intent = Intent(context!!, DetailsActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, item)

                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity!!,
                    (itemViewHolder.view as CustomImageCardView).mainImageView,
                    DetailsActivity.SHARED_ELEMENT_NAME
                )
                    .toBundle()
                startActivity(intent, bundle)
            } else if (item is String) {
                if (item.contains(getString(R.string.error_fragment))) {
                    val intent = Intent(context!!, BrowseErrorActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(context!!, item, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}