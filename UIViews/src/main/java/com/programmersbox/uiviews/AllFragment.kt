package com.programmersbox.uiviews

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeDiffUtil
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.utils.EndlessScrollingListener
import com.programmersbox.uiviews.utils.FirebaseDb
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass.
 * Use the [AllFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllFragment : BaseListFragment() {

    private val disposable: CompositeDisposable = CompositeDisposable()
    private var count = 1

    override val layoutId: Int get() = R.layout.fragment_all

    private val currentList = mutableListOf<ItemModel>()

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val itemListener = FirebaseDb.FirebaseListener()

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        super.viewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.allList)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.allRefresh)
        val editText = view.findViewById<TextInputEditText>(R.id.search_info)

        Flowables.combineLatest(
            itemListener.getAllShowsFlowable(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.maxByOrNull(DbModel::numChapters)!! } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { adapter.update(it) { s, d -> s.url == d.url } }
            .addTo(disposable)

        rv?.apply {
            adapter = this@AllFragment.adapter
            layoutManager = createLayoutManager(this@AllFragment.requireContext())
            addOnScrollListener(object : EndlessScrollingListener(layoutManager!!) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                    if (sourcePublish.value!!.canScroll && editText.text.isNullOrEmpty()) {
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

        sourcePublish
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                count = 1
                adapter.setListNotify(emptyList())
                sourceLoad(it)
                rv?.scrollToPosition(0)
            }
            .addTo(disposable)

        view.findViewById<FloatingActionButton>(R.id.scrollToTop).setOnClickListener {
            lifecycleScope.launch {
                activity?.runOnUiThread { rv?.smoothScrollToPosition(0) }
                delay(500)
                activity?.runOnUiThread { rv?.scrollToPosition(0) }
            }
        }

        editText
            .textChanges()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(500, TimeUnit.MILLISECONDS)
            .flatMapSingle { sourcePublish.value!!.searchList(it, 1, currentList) }
            .subscribe {
                adapter.setData(it)
                activity?.runOnUiThread { view.findViewById<TextInputLayout>(R.id.search_layout)?.suffixText = "${it.size}" }
            }
            .addTo(disposable)

    }

    private fun DragSwipeAdapter<ItemModel, *>.setData(newList: List<ItemModel>) {
        val diffCallback = object : DragSwipeDiffUtil<ItemModel>(dataList, newList) {
            override fun areContentsTheSame(oldItem: ItemModel, newItem: ItemModel): Boolean = oldItem.url == newItem.url
            override fun areItemsTheSame(oldItem: ItemModel, newItem: ItemModel): Boolean = oldItem.url === newItem.url
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        dataList.clear()
        dataList.addAll(newList)
        runOnUIThread { diffResult.dispatchUpdatesTo(this) }
    }

    private fun sourceLoad(sources: ApiService, page: Int = 1) {
        val searchLayout = view?.findViewById<TextInputLayout>(R.id.search_layout)
        sources.getList(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                adapter.addItems(it)
                currentList.clear()
                currentList.addAll(it)
                view?.findViewById<SwipeRefreshLayout>(R.id.allRefresh)?.isRefreshing = false
                activity?.runOnUiThread {
                    searchLayout?.editText?.setText("")
                    searchLayout?.suffixText = "${adapter.dataList.size}"
                    searchLayout?.hint = getString(R.string.searchFor, sourcePublish.value?.serviceName.orEmpty())
                }
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
        fun newInstance() = AllFragment()
    }
}