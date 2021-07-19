package com.programmersbox.uiviews

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeDiffUtil
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.databinding.FragmentGlobalSearchBinding
import com.programmersbox.uiviews.databinding.SearchItemBinding
import com.programmersbox.uiviews.utils.toolTipText
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class GlobalSearchFragment : BaseFragment() {

    override val layoutId: Int get() = R.layout.fragment_global_search

    private val disposable: CompositeDisposable = CompositeDisposable()

    private val info: GenericInfo by inject()

    private val adapter by lazy { SearchAdapter() }

    private lateinit var binding: FragmentGlobalSearchBinding

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {

        binding = FragmentGlobalSearchBinding.bind(view)

        binding.scrollToTop.setOnClickListener {
            lifecycleScope.launch {
                activity?.runOnUiThread { binding.searchList.smoothScrollToPosition(0) }
                delay(500)
                activity?.runOnUiThread { binding.searchList.scrollToPosition(0) }
            }
        }

        binding.searchList.apply {
            adapter = this@GlobalSearchFragment.adapter
            setItemViewCacheSize(20)
            setHasFixedSize(true)
        }

        ReactiveNetwork.observeInternetConnectivity()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.offlineView.visibility = if (it) View.GONE else View.VISIBLE
                binding.searchRefresh.visibility = if (it) View.VISIBLE else View.GONE
            }
            .addTo(disposable)

        binding.searchRefresh.isEnabled = false

        binding.searchInfo
            .textChanges()
            .doOnNext { activity?.runOnUiThread { binding.searchRefresh.isRefreshing = true } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(1000, TimeUnit.MILLISECONDS)
            .flatMap { s ->
                Observable.combineLatest(
                    info.searchList()
                        .map {
                            it
                                .searchList(s, list = emptyList())
                                .timeout(5, TimeUnit.SECONDS)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .onErrorReturnItem(emptyList())
                                .toObservable()
                        }
                ) { (it as Array<List<ItemModel>>).toList().flatten().sortedBy(ItemModel::title) }
            }
            .onErrorReturnItem(emptyList())
            .subscribe {
                adapter.setData(it)
                binding.searchLayout.suffixText = "${it.size}"
                binding.searchRefresh.isRefreshing = false
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

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    inner class SearchAdapter : DragSwipeAdapter<ItemModel, SearchHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHolder =
            SearchHolder(SearchItemBinding.inflate(requireContext().layoutInflater, parent, false))

        override fun SearchHolder.onBind(item: ItemModel, position: Int) = bind(item)
    }

    class SearchHolder(private val binding: SearchItemBinding) : RecyclerView.ViewHolder(binding.root), KoinComponent {

        private val logo: MainLogo by inject()

        fun bind(info: ItemModel) {
            binding.show = info
            binding.root.toolTipText(info.title)
            Glide.with(itemView.context)
                .asBitmap()
                .load(info.imageUrl)
                .fallback(logo.logoId)
                .placeholder(logo.logoId)
                .error(logo.logoId)
                .fitCenter()
                .transform(RoundedCorners(15))
                .into(binding.galleryListCover)

            binding.root.setOnClickListener {
                binding.root.findNavController()
                    .navigate(GlobalSearchFragmentDirections.actionGlobalSearchFragmentToDetailsFragment(info))
            }
            binding.executePendingBindings()
        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = GlobalSearchFragment()
    }
}