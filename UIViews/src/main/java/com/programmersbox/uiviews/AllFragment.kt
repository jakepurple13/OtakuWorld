package com.programmersbox.uiviews

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeDiffUtil
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.databinding.FragmentAllBinding
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

    private lateinit var binding: FragmentAllBinding

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        super.viewCreated(view, savedInstanceState)

        binding = FragmentAllBinding.bind(view)

        Flowables.combineLatest(
            itemListener.getAllShowsFlowable(),
            dao.getAllFavorites()
        ) { f, d -> (f + d).groupBy(DbModel::url).map { it.value.maxByOrNull(DbModel::numChapters)!! } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { adapter.update(it) { s, d -> s.url == d.url } }
            .addTo(disposable)

        binding.allList.apply {
            adapter = this@AllFragment.adapter
            layoutManager = createLayoutManager(this@AllFragment.requireContext())
            addOnScrollListener(object : EndlessScrollingListener(layoutManager!!) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                    if (sourcePublish.value!!.canScroll && binding.searchInfo.text.isNullOrEmpty()) {
                        count++
                        binding.allRefresh.isRefreshing = true
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
                binding.allRefresh.visibility = if (it) View.VISIBLE else View.GONE
            }
            .addTo(disposable)

        sourcePublish
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                count = 1
                adapter.setListNotify(emptyList())
                sourceLoad(it)
                binding.allList.scrollToPosition(0)
            }
            .addTo(disposable)

        binding.scrollToTop.setOnClickListener {
            lifecycleScope.launch {
                activity?.runOnUiThread { binding.allList.smoothScrollToPosition(0) }
                delay(500)
                activity?.runOnUiThread { binding.allList.scrollToPosition(0) }
            }
        }

        binding.searchInfo
            .textChanges()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(500, TimeUnit.MILLISECONDS)
            .flatMapSingle { sourcePublish.value!!.searchList(it, 1, currentList) }
            .subscribe {
                adapter.setData(it)
                activity?.runOnUiThread { binding.searchLayout.suffixText = "${it.size}" }
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
        sources.getList(page)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                adapter.addItems(it)
                currentList.clear()
                currentList.addAll(it)
                binding.allRefresh.isRefreshing = false
                activity?.runOnUiThread {
                    binding.searchLayout.editText?.setText("")
                    binding.searchLayout.suffixText = "${adapter.dataList.size}"
                    binding.searchLayout.hint = getString(R.string.searchFor, sourcePublish.value?.serviceName.orEmpty())
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