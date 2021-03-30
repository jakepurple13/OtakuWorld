package com.programmersbox.uiviews

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.loggingutils.Loged
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.utils.EndlessScrollingListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        super.viewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.recentList)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.recentRefresh)

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
    }

    companion object {
        @JvmStatic
        fun newInstance() = RecentFragment()
    }
}