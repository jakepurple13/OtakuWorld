package com.programmersbox.uiviews

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.programmersbox.dragswipe.CheckAdapter
import com.programmersbox.dragswipe.CheckAdapterInterface
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.models.ChapterModel
import com.programmersbox.uiviews.databinding.ChapterItemBinding
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChapterAdapter(private val context: Context, private val genericInfo: GenericInfo) :
    DragSwipeAdapter<ChapterModel, ChapterAdapter.ChapterHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(ChapterItemBinding.inflate(context.layoutInflater, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) = bind(item)

    inner class ChapterHolder(private val binding: ChapterItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chapterModel: ChapterModel) {
            binding.chapter = chapterModel
            binding.executePendingBindings()
            binding.chapterListCard.setOnClickListener { genericInfo.chapterOnClick(chapterModel, context) }
        }
    }
}