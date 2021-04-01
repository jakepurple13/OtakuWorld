package com.programmersbox.mangaworld

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestBuilder
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import com.github.piasy.biv.view.BigImageView
import com.google.android.material.button.MaterialButton
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.ChapterModel
import com.programmersbox.thirdpartyutils.DragSwipeGlideAdapter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class PageAdapter(
    override val fullRequest: RequestBuilder<Drawable>,
    override val thumbRequest: RequestBuilder<Drawable>,
    private val activity: AppCompatActivity,
    dataList: MutableList<String>,
    private val chapterModels: List<ChapterModel>,
    var currentChapter: Int,
    private val mangaUrl: String,
    private val loadNewPages: (ChapterModel) -> Unit = {},
    private val canDownload: (String) -> Unit = { }
) : DragSwipeGlideAdapter<String, PageHolder, String>(dataList) {

    private val context: Context = activity

    //private val dao by lazy { MangaDatabase.getInstance(context).mangaDao() }

    override val itemToModel: (String) -> String = { it }

    //private val ad by lazy { AdRequest.Builder().build() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder = context.layoutInflater.inflate(viewType, parent, false).let {
        when (viewType) {
            R.layout.page_end_chapter_item -> PageHolder.LastChapterHolder(it)
            R.layout.page_next_chapter_item -> PageHolder.LoadNextChapterHolder(it)
            R.layout.page_item -> PageHolder.ReadingHolder(it)
            else -> PageHolder.ReadingHolder(it)
        }
    }

    override fun getItemCount(): Int = super.getItemCount() + 1

    override fun getItemViewType(position: Int): Int = when {
        position == dataList.size && currentChapter <= 0 -> R.layout.page_end_chapter_item
        position == dataList.size -> R.layout.page_next_chapter_item
        else -> R.layout.page_item
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        when (holder) {
            is PageHolder.ReadingHolder -> holder.render(dataList[position], canDownload)
            is PageHolder.LoadNextChapterHolder -> {
                holder.render(activity) {
                    runOnUIThread {
                        chapterModels.getOrNull(--currentChapter)?.let(loadNewPages)
                        /*chapterModels.getOrNull(currentChapter)?.let { item ->
                            MangaReadChapter(item.url, item.name, mangaUrl)
                                .let {
                                    Completable.mergeArray(
                                        FirebaseDb.addChapter(it),
                                        dao.insertChapter(it)
                                    )
                                }
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribe()
                        }*/
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

}

sealed class PageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class LastChapterHolder(itemView: View) : PageHolder(itemView) {
        private val returnButton = itemView.findViewById<MaterialButton>(R.id.goBackFromReading)
        fun render(activity: AppCompatActivity) {
            //itemView.adViewEnd.loadAd(request)
            returnButton?.setOnClickListener { activity.finish() }
        }
    }

    class ReadingHolder(itemView: View) : PageHolder(itemView) {
        private val image by lazy { itemView.findViewById<BigImageView>(R.id.chapterPage) }

        fun render(item: String?, canDownload: (String) -> Unit) {
            image?.setProgressIndicator(ProgressPieIndicator())
            image?.showImage(Uri.parse(item), Uri.parse(item))
            /*image.setOnLongClickListener {
                try {
                    MaterialAlertDialogBuilder(itemView.context)
                        .setTitle(itemView.context.getText(R.string.downloadPage))
                        .setPositiveButton(itemView.context.getText(android.R.string.ok)) { d, _ -> canDownload(item!!);d.dismiss() }
                        .setNegativeButton(itemView.context.getText(R.string.fui_cancel)) { d, _ -> d.dismiss() }
                        .show()
                } catch (e: Exception) {
                }
                true
            }*/
        }
    }

    class LoadNextChapterHolder(itemView: View) : PageHolder(itemView) {
        private val loadButton = itemView.findViewById<MaterialButton>(R.id.loadNextChapter)
        private val returnButton = itemView.findViewById<MaterialButton>(R.id.goBackFromReadingLoad)
        fun render(activity: AppCompatActivity, load: suspend () -> Unit) {
            //itemView.adViewNext.loadAd(request)
            loadButton?.setOnClickListener { GlobalScope.launch { load() } }
            returnButton?.setOnClickListener { activity.finish() }
        }
    }

}