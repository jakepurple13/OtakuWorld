package com.programmersbox.uiviews

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.uiviews.databinding.ChapterItemBinding

class ChapterAdapter(private val context: Context, private val genericInfo: GenericInfo, private val dao: ItemDao) :
    DragSwipeAdapter<ChapterModel, ChapterAdapter.ChapterHolder>() {

    var swatchInfo: SwatchInfo? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(ChapterItemBinding.inflate(context.layoutInflater, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) = bind(item)

    inner class ChapterHolder(private val binding: ChapterItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chapterModel: ChapterModel) {
            binding.chapter = chapterModel
            binding.swatch = swatchInfo
            binding.executePendingBindings()
            binding.chapterListCard.setOnClickListener { genericInfo.chapterOnClick(chapterModel, dataList, context) }
        }
    }
}