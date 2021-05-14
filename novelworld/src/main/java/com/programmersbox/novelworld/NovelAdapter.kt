package com.programmersbox.novelworld

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ItemModel
import com.programmersbox.novelworld.databinding.NovelListItemBinding
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.ItemListAdapter

class NovelAdapter(
    context: Context,
    baseListFragment: BaseListFragment,
) : ItemListAdapter<NovelHolder>(context, baseListFragment) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelHolder =
        NovelHolder(NovelListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun NovelHolder.onBind(item: ItemModel, position: Int) {
        bind(item, currentList)
        itemView.setOnClickListener { onClick(it, item) }
    }
}

class NovelHolder(private val binding: NovelListItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(info: ItemModel, list: List<DbModel>) {
        binding.show = info
        /*binding.favoriteHeart.changeTint(binding.animeTitle.currentTextColor)
        binding.favoriteHeart.check(false)
        binding.favoriteHeart.check(list.any { it.url == info.url })*/
        binding.executePendingBindings()
    }

}