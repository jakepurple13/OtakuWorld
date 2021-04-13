package com.programmersbox.uiviews

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.models.ApiService
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.utils.EndlessScrollingListener
import com.programmersbox.uiviews.utils.FirebaseDb
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

/**
 * A simple [Fragment] subclass.
 * Use the [RecentFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecentFragment : BaseListFragment() {

    override val layoutId: Int get() = R.layout.fragment_recent

    private val disposable: CompositeDisposable = CompositeDisposable()

    private var count = 1

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val itemListener = FirebaseDb.FirebaseListener()

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        super.viewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.recentList)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.recentRefresh)

        Flowables.combineLatest(
            itemListener.getAllShowsFlowable(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.maxByOrNull(DbModel::numChapters)!! } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { adapter.update(it) { s, d -> s.url == d.url } }
            .addTo(disposable)

        rv?.apply {
            adapter = this@RecentFragment.adapter
            layoutManager = createLayoutManager(this@RecentFragment.requireContext())
            addOnScrollListener(object : EndlessScrollingListener(layoutManager!!) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                    if (sourcePublish.value!!.canScroll) {
                        count++
                        refresh.isRefreshing = true
                        sourceLoad(sourcePublish.value!!, count)
                    }
                }
            })
        }

        ReactiveNetwork.observeInternetConnectivity()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                view.findViewById<RelativeLayout>(R.id.offline_view).visibility = if (it) View.GONE else View.VISIBLE
                refresh.visibility = if (it) View.VISIBLE else View.GONE
            }
            .addTo(disposable)

        refresh.setOnRefreshListener { sourceLoad(sourcePublish.value!!) }

        sourcePublish
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                count = 1
                adapter.setListNotify(emptyList())
                sourceLoad(it)
            }
            .addTo(disposable)

    }

    private fun sourceLoad(sources: ApiService, page: Int = 1) {
        sources
            .getRecent(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                adapter.addItems(it)
                view?.findViewById<SwipeRefreshLayout>(R.id.recentRefresh)?.isRefreshing = false
            }
            .addTo(disposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        itemListener.unregister()
    }

    companion object {
        @JvmStatic
        fun newInstance() = RecentFragment()
    }
}