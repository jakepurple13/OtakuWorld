package com.programmersbox.uiviews

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.programmersbox.dragswipe.CheckAdapter
import com.programmersbox.dragswipe.CheckAdapterInterface
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.uiviews.databinding.ChapterItemBinding
import com.programmersbox.uiviews.utils.FirebaseDb
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ChapterAdapter(
    private val context: Context,
    private val genericInfo: GenericInfo,
    private val dao: ItemDao,
    check: CheckAdapter<ChapterModel, ChapterWatched> = CheckAdapter()
) : DragSwipeAdapter<ChapterModel, ChapterAdapter.ChapterHolder>(), CheckAdapterInterface<ChapterModel, ChapterWatched> by check {

    init {
        check.adapter = this
    }

    var swatchInfo: SwatchInfo? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var itemUrl: String? = null
    var title: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(ChapterItemBinding.inflate(context.layoutInflater, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) = bind(item)

    inner class ChapterHolder(val binding: ChapterItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chapterModel: ChapterModel) {
            binding.chapter = chapterModel
            binding.swatch = swatchInfo
            binding.executePendingBindings()
            binding.readChapterButton.setOnClickListener { binding.chapterListCard.performClick() }
            //binding.markedReadButton.setOnClickListener { binding.readChapter.performClick() }
            //binding.uploadedInfo2.setOnClickListener { binding.chapterListCard.performClick() }
            binding.chapterListCard.setOnClickListener {
                genericInfo.chapterOnClick(chapterModel, dataList, context)
                binding.readChapter.isChecked = true
            }
            binding.downloadChapterButton.setOnClickListener {
                genericInfo.downloadChapter(chapterModel, title.orEmpty())
                binding.readChapter.isChecked = true
            }
            binding.readChapter.setOnCheckedChangeListener(null)
            binding.readChapter.isChecked = currentList.any { it.url == chapterModel.url }
            binding.readChapter.setOnCheckedChangeListener { _, b ->
                itemUrl?.let { ChapterWatched(url = chapterModel.url, name = chapterModel.name, favoriteUrl = it) }
                    ?.let {
                        Completable.mergeArray(
                            if (b) FirebaseDb.insertEpisodeWatched(it) else FirebaseDb.removeEpisodeWatched(it),
                            if (b) dao.insertChapter(it) else dao.deleteChapter(it)
                        )
                    }
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe {
                        Snackbar.make(
                            binding.root,
                            if (b) R.string.addedChapterItem else R.string.removedChapterItem,
                            Snackbar.LENGTH_SHORT
                        )
                            .setAction(R.string.undo) { binding.readChapter.isChecked = !binding.readChapter.isChecked }
                            .show()
                    }
            }

        }
    }
}