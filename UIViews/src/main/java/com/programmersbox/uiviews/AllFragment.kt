package com.programmersbox.uiviews

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.widget.RxTextView.textChanges
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeDiffUtil
import com.programmersbox.dragswipe.setData
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass.
 * Use the [AllFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllFragment() : BaseListFragment() {

    private val disposable: CompositeDisposable = CompositeDisposable()
    private var count = 1

    override val layoutId: Int get() = R.layout.fragment_all

    private val currentList = mutableListOf<ItemModel>()

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {
        super.viewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.allList)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.allRefresh)

        rv?.apply {
            adapter = this@AllFragment.adapter
            layoutManager = createLayoutManager(this@AllFragment.requireContext())
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

        view.findViewById<TextInputEditText>(R.id.search_info)
            .textChanges()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(500, TimeUnit.MILLISECONDS)
            .flatMapSingle { sourcePublish.value!!.searchList(it, 1, currentList) }
            .subscribe {
                adapter.setData(it)
                activity?.runOnUiThread { view?.findViewById<TextInputLayout>(R.id.search_layout)?.suffixText = "${it.size}" }
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
                view?.findViewById<SwipeRefreshLayout>(R.id.allRefresh)?.isRefreshing = false
                activity?.runOnUiThread {
                    view?.findViewById<TextInputLayout>(R.id.search_layout)?.suffixText = "${adapter.dataList.size}"
                    //search_layout?.hint = "Search: ${sourcePublish.value?.name}"
                }
            }
            .addTo(disposable)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AllFragment()
    }
}