package com.programmersbox.uiviews

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeDiffUtil
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ApiService
import com.programmersbox.rxutils.behaviorDelegate
import com.programmersbox.rxutils.toLatestFlowable
import com.programmersbox.uiviews.databinding.FavoriteItemBinding
import com.programmersbox.uiviews.utils.AutoFitGridLayoutManager
import com.programmersbox.uiviews.utils.FirebaseDb
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class FavoriteFragment : BaseFragment() {

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val disposable = CompositeDisposable()

    private val sources by lazy { BaseMainActivity.genericInfo.sourceList() }
    private val sourcePublisher = BehaviorSubject.createDefault(sources.toMutableList())
    private var sourcesList by behaviorDelegate(sourcePublisher)
    private val adapter by lazy { FavoriteAdapter() }

    private val fireListener = FirebaseDb.FirebaseListener()

    override val layoutId: Int get() = R.layout.fragment_favorite

    override fun viewCreated(view: View, savedInstanceState: Bundle?) {

        uiSetup(view)

        val favRv = view.findViewById<RecyclerView>(R.id.favRv)

        val fired = fireListener.getAllShowsFlowable()

        val dbFire = Flowables.combineLatest(
            fired,
            dao.getAllFavorites()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        ) { fire, db -> (db + fire).groupBy(DbModel::url).map { it.value.maxByOrNull(DbModel::numChapters)!! } }

        Flowables.combineLatest(
            source1 = dbFire
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()),
            source2 = sourcePublisher.toLatestFlowable(),
            source3 = view.findViewById<TextInputEditText>(R.id.fav_search_info)
                .textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { pair ->
                pair.first.sortedBy(DbModel::title)
                    .filter { it.source in pair.second.map { it.serviceName } && it.title.contains(pair.third, true) }
            }
            .map { it.size to it.toGroup() }
            .distinctUntilChanged()
            .subscribe {
                adapter.setData(it.second.toList())
                view.findViewById<TextInputLayout>(R.id.fav_search_layout)?.hint =
                    resources.getQuantityString(R.plurals.numFavorites, it.first, it.first)
                favRv?.smoothScrollToPosition(0)
            }
            .addTo(disposable)

        val sourceList = view.findViewById<ChipGroup>(R.id.sourceList)

        dbFire
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.groupBy { s -> s.source } }
            .subscribe { s ->
                s.forEach { m -> sourceList.children.filterIsInstance<Chip>().find { it.text == m.key }?.text = "${m.key}: ${m.value.size}" }
            }
            .addTo(disposable)

    }

    private fun List<DbModel>.toGroup() = groupBy(DbModel::title)

    private fun uiSetup(view: View) = with(view) {
        val favRv = view.findViewById<RecyclerView>(R.id.favRv)
        favRv.layoutManager = AutoFitGridLayoutManager(requireContext(), 360).apply { orientation = GridLayoutManager.VERTICAL }
        favRv.adapter = adapter
        favRv.setItemViewCacheSize(20)
        favRv.setHasFixedSize(true)

        val sourceList = findViewById<ChipGroup>(R.id.sourceList)

        sourceList.addView(Chip(requireContext()).apply {
            text = "ALL"
            isCheckable = true
            isClickable = true
            isChecked = true
            setOnClickListener { sourceList.children.filterIsInstance<Chip>().forEach { it.isChecked = true } }
        })

        sources.forEach {
            sourceList.addView(Chip(requireContext()).apply {
                text = it.serviceName
                isCheckable = true
                isClickable = true
                isChecked = true
                setOnCheckedChangeListener { _, isChecked -> addOrRemoveSource(isChecked, it) }
                setOnLongClickListener {
                    sourceList.clearCheck()
                    isChecked = true
                    true
                }
            })
        }
    }

    override fun onDestroy() {
        disposable.dispose()
        fireListener.unregister()
        super.onDestroy()
    }

    private fun addOrRemoveSource(isChecked: Boolean, sources: ApiService) {
        sourcesList = sourcesList?.apply { if (isChecked) add(sources) else remove(sources) }
    }

    private fun DragSwipeAdapter<Pair<String, List<DbModel>>, *>.setData(newList: List<Pair<String, List<DbModel>>>) {
        val diffCallback = object : DragSwipeDiffUtil<Pair<String, List<DbModel>>>(dataList, newList) {
            override fun areContentsTheSame(oldItem: Pair<String, List<DbModel>>, newItem: Pair<String, List<DbModel>>): Boolean =
                oldItem.second == newItem.second

            override fun areItemsTheSame(oldItem: Pair<String, List<DbModel>>, newItem: Pair<String, List<DbModel>>): Boolean =
                oldItem.second === newItem.second
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        dataList.clear()
        dataList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class FavoriteAdapter : DragSwipeAdapter<Pair<String, List<DbModel>>, FavoriteHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteHolder =
            FavoriteHolder(FavoriteItemBinding.inflate(requireContext().layoutInflater, parent, false))

        override fun FavoriteHolder.onBind(item: Pair<String, List<DbModel>>, position: Int) = bind(item.second)
    }

    class FavoriteHolder(private val binding: FavoriteItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(info: List<DbModel>) {
            binding.show = info.random()
            Glide.with(itemView.context)
                .asBitmap()
                .load(info.random().imageUrl)
                .fallback(OtakuApp.logo)
                .placeholder(OtakuApp.logo)
                .error(OtakuApp.logo)
                .fitCenter()
                .transform(RoundedCorners(15))
                .into(binding.galleryListCover)

            binding.root.setOnClickListener {
                if (info.size == 1) {
                    val item = info.firstOrNull()?.let { BaseMainActivity.genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                    binding.root.findNavController().navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                } else {
                    MaterialAlertDialogBuilder(itemView.context)
                        .setTitle(R.string.chooseASource)
                        .setItems(info.map { "${it.source} - ${it.title}" }.toTypedArray()) { d, i ->
                            val item = info[i].let { BaseMainActivity.genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                            binding.root.findNavController().navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                            d.dismiss()
                        }
                        .show()
                }
            }
            binding.executePendingBindings()
        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = FavoriteFragment()
    }
}