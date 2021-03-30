package com.programmersbox.animeworld

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.anime_sources.Sources
import com.programmersbox.animeworld.databinding.AnimeListItemBinding
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.ItemListAdapter

class MainActivity : BaseMainActivity() {

    override fun createGenericInfo(): GenericInfo = object : GenericInfo {

        override fun createAdapter(context: Context, baseListFragment: BaseListFragment): ItemListAdapter<RecyclerView.ViewHolder> =
            (AnimeAdapter(context, baseListFragment) as ItemListAdapter<RecyclerView.ViewHolder>)

        override fun createLayoutManager(context: Context): RecyclerView.LayoutManager = LinearLayoutManager(context)

        override fun chapterOnClick(model: ChapterModel, context: Context) {

        }

    }

    override fun onCreate() {

        sourcePublish.onNext(Sources.GOGOANIME)

    }

}

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