package com.programmersbox.animeworld

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.animeworld.databinding.AnimeListItemBinding
import com.programmersbox.dragswipe.CheckAdapter
import com.programmersbox.dragswipe.CheckAdapterInterface
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ItemModel
import com.programmersbox.thirdpartyutils.changeTint
import com.programmersbox.thirdpartyutils.check
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.ItemListAdapter

class AnimeAdapter(
    context: Context,
    baseListFragment: BaseListFragment,
    check: CheckAdapter<ItemModel, DbModel> = CheckAdapter()
) : ItemListAdapter<AnimeHolder>(context, baseListFragment), CheckAdapterInterface<ItemModel, DbModel> by check {

    init {
        check.adapter = this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeHolder =
        AnimeHolder(AnimeListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun AnimeHolder.onBind(item: ItemModel, position: Int) {
        bind(item, currentList)
        itemView.setOnClickListener { onClick(it, item) }
    }
}

class AnimeHolder(private val binding: AnimeListItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(info: ItemModel, list: List<DbModel>) {
        binding.show = info
        binding.favoriteHeart.changeTint(binding.animeTitle.currentTextColor)
        binding.favoriteHeart.check(false)
        binding.favoriteHeart.check(list.any { it.url == info.url })
        binding.executePendingBindings()
    }

}