package com.programmersbox.mangaworld.reader

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestBuilder
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import com.google.android.material.snackbar.Snackbar
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.databinding.PageEndChapterItemBinding
import com.programmersbox.mangaworld.databinding.PageItemBinding
import com.programmersbox.mangaworld.databinding.PageNextChapterItemBinding
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.utils.DragSwipeGlideAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Collections

class PageAdapter(
    override val fullRequest: RequestBuilder<Drawable>,
    override val thumbRequest: RequestBuilder<Drawable>,
    private val activity: AppCompatActivity,
    dataList: MutableList<String>,
    private val onTap: () -> Unit,
    private val coordinatorLayout: CoordinatorLayout,
    private val chapterModels: List<KmpChapterModel>,
    var currentChapter: Int,
    private val mangaUrl: String,
    private val loadNewPages: (KmpChapterModel) -> Unit = {},
) : DragSwipeGlideAdapter<String, PageHolder, String>(dataList) {

    private val context: Context = activity

    private val dao by lazy { ItemDatabase.getInstance(DatabaseBuilder(context)).itemDao() }

    override val itemToModel: (String) -> String = { it }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder = when (viewType) {
        R.layout.page_end_chapter_item -> PageHolder.LastChapterHolder(PageEndChapterItemBinding.inflate(context.layoutInflater, parent, false))
        R.layout.page_next_chapter_item -> PageHolder.LoadNextChapterHolder(PageNextChapterItemBinding.inflate(context.layoutInflater, parent, false))
        R.layout.page_item -> PageHolder.ReadingHolder(PageItemBinding.inflate(context.layoutInflater, parent, false))
        else -> PageHolder.ReadingHolder(PageItemBinding.inflate(context.layoutInflater, parent, false))
    }

    override fun getItemCount(): Int = super.getItemCount() + 1

    override fun getItemViewType(position: Int): Int = when {
        position == dataList.size && currentChapter <= 0 -> R.layout.page_end_chapter_item
        position == dataList.size -> R.layout.page_next_chapter_item
        else -> R.layout.page_item
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        when (holder) {
            is PageHolder.ReadingHolder -> holder.render(dataList[position], onTap)
            is PageHolder.LoadNextChapterHolder -> {
                holder.render(activity) {
                    //Glide.get(activity).clearMemory()
                    chapterModels.getOrNull(--currentChapter)
                        ?.also(loadNewPages)
                        ?.let { item ->
                            activity.lifecycleScope.launch {
                                ChapterWatched(item.url, item.name, mangaUrl)
                                    .let {
                                        dao.insertChapter(it)
                                        FirebaseDb.insertEpisodeWatchedFlow(it).collect()
                                    }
                            }
                            Snackbar.make(
                                coordinatorLayout,
                                R.string.addedChapterItem,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                }
            }

            is PageHolder.LastChapterHolder -> holder.render(activity)
        }
    }

    override fun PageHolder.onBind(item: String, position: Int) = Unit

    fun reloadChapter() {
        chapterModels.getOrNull(currentChapter)?.let(loadNewPages)
    }

    //override fun getPreloadItems(position: Int): List<String> = Collections.singletonList(dataList[position].let(itemToModel))

    override fun getPreloadItems(position: Int): List<String> = Collections.singletonList(dataList.getOrNull(position)?.let(itemToModel).orEmpty())
    override fun getPreloadRequestBuilder(item: String): RequestBuilder<Drawable?> = fullRequest.thumbnail(thumbRequest.load(item)).load(item)

}

sealed class PageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class LastChapterHolder(private val binding: PageEndChapterItemBinding) : PageHolder(binding.root) {
        fun render(activity: AppCompatActivity) {
            binding.goBackFromReading.setOnClickListener { activity.finish() }
        }
    }

    class ReadingHolder(private val binding: PageItemBinding) : PageHolder(binding.root) {
        fun render(item: String?, onTap: () -> Unit) {
            binding.chapterPage.setProgressIndicator(ProgressPieIndicator())
            binding.chapterPage.setOnClickListener { onTap() }
            binding.chapterPage.showImage(Uri.parse(item), Uri.parse(item))
        }
    }

    class LoadNextChapterHolder(private val binding: PageNextChapterItemBinding) : PageHolder(binding.root) {
        fun render(activity: AppCompatActivity, load: () -> Unit) {
            binding.loadNextChapter.setOnClickListener { load() }
            binding.goBackFromReadingLoad.setOnClickListener { activity.finish() }
        }
    }

}