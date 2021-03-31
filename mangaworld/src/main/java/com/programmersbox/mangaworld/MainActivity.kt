package com.programmersbox.mangaworld

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.manga_sources.Sources
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.ItemListAdapter
import com.programmersbox.uiviews.utils.AutoFitGridLayoutManager
import com.programmersbox.uiviews.utils.currentService
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class MainActivity : BaseMainActivity() {

    override fun onCreate() {

        if(currentService == null) {
            sourcePublish.onNext(Sources.NINE_ANIME)
            currentService = Sources.NINE_ANIME
        }
    }

    override fun createAdapter(context: Context, baseListFragment: BaseListFragment): ItemListAdapter<RecyclerView.ViewHolder> =
        (MangaGalleryAdapter(context, baseListFragment) as ItemListAdapter<RecyclerView.ViewHolder>)

    override fun createLayoutManager(context: Context): RecyclerView.LayoutManager =
        AutoFitGridLayoutManager(context, 360).apply { orientation = GridLayoutManager.VERTICAL }

    override fun chapterOnClick(model: ChapterModel, context: Context) {

        model.getChapterInfo()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribeBy { println(it) }
            .addTo(disposable)

    }

    override fun sourceList(): List<ApiService> = Sources.values().toList()

    override fun toSource(s: String): ApiService? = try {
        Sources.valueOf(s)
    } catch (e: IllegalArgumentException) {
        null
    }

}
