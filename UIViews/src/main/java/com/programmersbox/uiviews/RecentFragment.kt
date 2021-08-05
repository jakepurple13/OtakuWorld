package com.programmersbox.uiviews

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.gone
import com.programmersbox.helpfulutils.visible
import com.programmersbox.models.ApiService
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.databinding.FragmentRecentBinding
import com.programmersbox.uiviews.utils.EndlessScrollingListener
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

    private lateinit var binding: FragmentRecentBinding

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        super.viewCreated(view, savedInstanceState)
        binding = FragmentRecentBinding.bind(view)

        Flowables.combineLatest(
            itemListener.getAllShowsFlowable(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.maxByOrNull(DbModel::numChapters)!! } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { adapter.update(it) { s, d -> s.url == d.url } }
            .addTo(disposable)

        binding.recentList.apply {
            adapter = this@RecentFragment.adapter
            layoutManager = info.createLayoutManager(this@RecentFragment.requireContext())
            addOnScrollListener(object : EndlessScrollingListener(layoutManager!!) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                    if (sourcePublish.value!!.canScroll) {
                        count++
                        binding.recentRefresh.isRefreshing = true
                        sourceLoad(sourcePublish.value!!, count)
                    }
                }
            })
        }

        ReactiveNetwork.observeInternetConnectivity()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.offlineView.visibility = if (it) View.GONE else View.VISIBLE
                binding.recentRefresh.visibility = if (it) View.VISIBLE else View.GONE
            }
            .addTo(disposable)

        binding.recentRefresh.setOnRefreshListener {
            binding.composeShimmer.visible()
            count = 1
            adapter.setListNotify(emptyList())
            sourceLoad(sourcePublish.value!!)
            binding.recentList.scrollToPosition(0)
        }

        binding.composeShimmer.setContent { MdcTheme { info.ComposeShimmerItem() } }

        sourcePublish
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.composeShimmer.visible()
                count = 1
                adapter.setListNotify(emptyList())
                sourceLoad(it)
                binding.recentList.scrollToPosition(0)
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
                binding.recentRefresh.isRefreshing = false
                binding.composeShimmer.gone()
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