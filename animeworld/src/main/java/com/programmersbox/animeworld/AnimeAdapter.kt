package com.programmersbox.animeworld

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.animeworld.databinding.AnimeListItemBinding
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.ItemListAdapter

class AnimeAdapter(context: Context, baseListFragment: BaseListFragment) :
    ItemListAdapter<AnimeHolder>(context, baseListFragment) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeHolder =
        AnimeHolder(AnimeListItemBinding.inflate(context.layoutInflater, parent, false))

    override fun AnimeHolder.onBind(item: ItemModel, position: Int) {
        bind(item)
        itemView.setOnClickListener { onClick(it, item) }
    }
}

class AnimeHolder(private val binding: AnimeListItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(info: ItemModel) {
        binding.show = info
        binding.executePendingBindings()
    }

}