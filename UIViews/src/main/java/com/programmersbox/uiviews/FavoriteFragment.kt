package com.programmersbox.uiviews

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding2.widget.textChanges
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.dragswipe.DragSwipeDiffUtil
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.helpfulutils.gone
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.visible
import com.programmersbox.models.ApiService
import com.programmersbox.rxutils.behaviorDelegate
import com.programmersbox.rxutils.toLatestFlowable
import com.programmersbox.uiviews.databinding.FavoriteItemBinding
import com.programmersbox.uiviews.databinding.FragmentFavoriteBinding
import com.programmersbox.uiviews.utils.AutoFitGridLayoutManager
import com.programmersbox.uiviews.utils.FirebaseDb
import com.programmersbox.uiviews.utils.MainLogo
import com.programmersbox.uiviews.utils.toolTipText
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class FavoriteFragment : BaseFragment() {

    private val dao by lazy { ItemDatabase.getInstance(requireContext()).itemDao() }
    private val disposable = CompositeDisposable()

    private val genericInfo by inject<GenericInfo>()
    private val sources by lazy { genericInfo.sourceList() }
    private val sourcePublisher = BehaviorSubject.createDefault(sources.toMutableList())
    private var sourcesList by behaviorDelegate(sourcePublisher)
    private val adapter by lazy { FavoriteAdapter() }

    private val fireListener = FirebaseDb.FirebaseListener()

    override val layoutId: Int get() = R.layout.fragment_favorite

    private val logo: MainLogo by inject()

    private lateinit var binding: FragmentFavoriteBinding

    @SuppressLint("SetTextI18n")
    override fun viewCreated(view: View, savedInstanceState: Bundle?) {

        binding = FragmentFavoriteBinding.bind(view)

        uiSetup()

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
            source3 = binding.favSearchInfo
                .textChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { pair ->
                pair.first.sortedBy(DbModel::title)
                    .filter { it.source in pair.second.map(ApiService::serviceName) && it.title.contains(pair.third, true) }
            }
            .map { it.size to it.toGroup() }
            .distinctUntilChanged()
            .subscribe {
                adapter.setData(it.second.toList())
                binding.favSearchLayout.hint = resources.getQuantityString(R.plurals.numFavorites, it.first, it.first)
                binding.favRv.smoothScrollToPosition(0)
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.gone()
                if (it.second.isNotEmpty()) binding.emptyState.gone()
                else binding.emptyState.visible()
            }
            .addTo(disposable)

        binding.viewRecentList.setOnClickListener { (activity as? BaseMainActivity)?.goToScreen(BaseMainActivity.Screen.RECENT) }

        dbFire
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.groupBy { s -> s.source } }
            .subscribe { s ->
                s.forEach { m -> binding.sourceList.children.filterIsInstance<Chip>().find { it.text == m.key }?.text = "${m.key}: ${m.value.size}" }
            }
            .addTo(disposable)

    }

    private fun List<DbModel>.toGroup() = groupBy(DbModel::title)

    @SuppressLint("SetTextI18n")
    private fun uiSetup() {
        binding.favRv.layoutManager = AutoFitGridLayoutManager(requireContext(), 360).apply { orientation = GridLayoutManager.VERTICAL }
        binding.favRv.adapter = adapter
        binding.favRv.setItemViewCacheSize(20)
        binding.favRv.setHasFixedSize(true)

        binding.sourceList.addView(Chip(requireContext()).apply {
            text = "ALL"
            isCheckable = true
            isClickable = true
            isChecked = true
            setOnClickListener { binding.sourceList.children.filterIsInstance<Chip>().forEach { it.isChecked = true } }
        })

        sources.forEach {
            binding.sourceList.addView(Chip(requireContext()).apply {
                text = it.serviceName
                isCheckable = true
                isClickable = true
                isChecked = true
                setOnCheckedChangeListener { _, isChecked -> addOrRemoveSource(isChecked, it) }
                setOnLongClickListener {
                    binding.sourceList.clearCheck()
                    isChecked = true
                    true
                }
            })
        }

        repeat(9) {
            binding.shimmerGrid.addView(
                ImageView(requireContext()).apply {
                    setImageResource(logo.logoId)
                    setBackgroundColor(0xB3B3B3)
                    setPadding(5)
                },
                360, 480
            )
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

        override fun FavoriteHolder.onBind(item: Pair<String, List<DbModel>>, position: Int) = bind(item.second, genericInfo)
    }

    class FavoriteHolder(private val binding: FavoriteItemBinding) : RecyclerView.ViewHolder(binding.root), KoinComponent {

        private val logo: MainLogo by inject()

        fun bind(info: List<DbModel>, genericInfo: GenericInfo) {
            binding.show = info.random()
            binding.root.toolTipText(info.random().title)
            Glide.with(itemView.context)
                .asBitmap()
                .load(info.random().imageUrl)
                .fallback(logo.logoId)
                .placeholder(logo.logoId)
                .error(logo.logoId)
                .fitCenter()
                .transform(RoundedCorners(15))
                .into(binding.galleryListCover)

            binding.root.setOnClickListener {
                if (info.size == 1) {
                    val item = info.firstOrNull()?.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                    binding.root.findNavController().navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                } else {
                    MaterialAlertDialogBuilder(itemView.context)
                        .setTitle(R.string.chooseASource)
                        .setItems(info.map { "${it.source} - ${it.title}" }.toTypedArray()) { d, i ->
                            val item = info[i].let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
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