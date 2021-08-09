package com.programmersbox.mangaworld

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestBuilder
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import com.google.android.gms.ads.AdRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.mangaworld.databinding.PageEndChapterItemBinding
import com.programmersbox.mangaworld.databinding.PageItemBinding
import com.programmersbox.mangaworld.databinding.PageNextChapterItemBinding
import com.programmersbox.models.ChapterModel
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.utils.DragSwipeGlideAdapter
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.util.*

class PageAdapter(
    private val disposable: CompositeDisposable,
    override val fullRequest: RequestBuilder<Drawable>,
    override val thumbRequest: RequestBuilder<Drawable>,
    private val activity: AppCompatActivity,
    dataList: MutableList<String>,
    private val onTap: () -> Unit,
    private val coordinatorLayout: CoordinatorLayout,
    private val chapterModels: List<ChapterModel>,
    var currentChapter: Int,
    private val mangaUrl: String,
    private val loadNewPages: (ChapterModel) -> Unit = {},
    private val canDownload: (String) -> Unit = { }
) : DragSwipeGlideAdapter<String, PageHolder, String>(dataList) {

    private val context: Context = activity

    private val dao by lazy { ItemDatabase.getInstance(context).itemDao() }

    override val itemToModel: (String) -> String = { it }

    private val ad by lazy { AdRequest.Builder().build() }

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
            is PageHolder.ReadingHolder -> holder.render(dataList[position], onTap, canDownload)
            is PageHolder.LoadNextChapterHolder -> {
                holder.render(activity, ad) {
                    //Glide.get(activity).clearMemory()
                    chapterModels.getOrNull(--currentChapter)
                        ?.also(loadNewPages)
                        ?.let { item ->
                            ChapterWatched(item.url, item.name, mangaUrl)
                                .let {
                                    Completable.mergeArray(
                                        FirebaseDb.insertEpisodeWatched(it),
                                        dao.insertChapter(it)
                                    )
                                }
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribe {
                                    Snackbar.make(
                                        coordinatorLayout,
                                        R.string.addedChapterItem,
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                                .addTo(disposable)
                        }
                }
            }
            is PageHolder.LastChapterHolder -> holder.render(activity, ad)
        }
    }

    override fun PageHolder.onBind(item: String, position: Int) = Unit

    fun reloadChapter() {
        chapterModels.getOrNull(currentChapter)?.let(loadNewPages)
    }

    //override fun getPreloadItems(position: Int): List<String> = Collections.singletonList(dataList[position].let(itemToModel))

    override fun getPreloadItems(position: Int): List<String> = Collections.singletonList(dataList.getOrNull(position)?.let(itemToModel).orEmpty())

}

sealed class PageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class LastChapterHolder(private val binding: PageEndChapterItemBinding) : PageHolder(binding.root) {
        fun render(activity: AppCompatActivity, request: AdRequest) {
            binding.adViewEnd.loadAd(request)
            binding.goBackFromReading.setOnClickListener { activity.finish() }
        }
    }

    class ReadingHolder(private val binding: PageItemBinding) : PageHolder(binding.root) {
        fun render(item: String?, onTap: () -> Unit, canDownload: (String) -> Unit) {
            binding.chapterPage.setProgressIndicator(ProgressPieIndicator())
            binding.chapterPage.setOnClickListener { onTap() }
            binding.chapterPage.showImage(Uri.parse(item), Uri.parse(item))
            binding.chapterPage.setOnLongClickListener {
                try {
                    MaterialAlertDialogBuilder(binding.root.context)
                        .setTitle(itemView.context.getText(R.string.downloadPage))
                        .setPositiveButton(binding.root.context.getText(android.R.string.ok)) { d, _ ->
                            canDownload(item!!)
                            d.dismiss()
                        }
                        .setNegativeButton(itemView.context.getText(R.string.no)) { d, _ -> d.dismiss() }
                        .show()
                } catch (e: Exception) {
                }
                true
            }
        }
    }

    class LoadNextChapterHolder(private val binding: PageNextChapterItemBinding) : PageHolder(binding.root) {
        fun render(activity: AppCompatActivity, request: AdRequest, load: () -> Unit) {
            binding.adViewNext.loadAd(request)
            binding.loadNextChapter.setOnClickListener { load() }
            binding.goBackFromReadingLoad.setOnClickListener { activity.finish() }
        }
    }

}