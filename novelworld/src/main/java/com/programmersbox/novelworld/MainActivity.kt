package com.programmersbox.novelworld

import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.novel_sources.Sources
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.ItemListAdapter
import com.programmersbox.uiviews.utils.ChapterModelSerializer
import com.programmersbox.uiviews.utils.currentService

class MainActivity : BaseMainActivity() {
    override fun onCreate() {

        if (currentService == null) {
            val s = Sources.values().random()

            sourcePublish.onNext(s)
            currentService = s.serviceName
        }

    }

    override fun createAdapter(context: Context, baseListFragment: BaseListFragment): ItemListAdapter<RecyclerView.ViewHolder> =
        (NovelAdapter(context, baseListFragment) as ItemListAdapter<RecyclerView.ViewHolder>)

    override fun createLayoutManager(context: Context): RecyclerView.LayoutManager = LinearLayoutManager(context)

    override fun chapterOnClick(model: ChapterModel, allChapters: List<ChapterModel>, context: Context) {
        startActivity(
            Intent(this, ReadingActivity::class.java).apply {
                putExtra("model", model.toJson(ChapterModel::class.java to ChapterModelSerializer()))
            }
        )
    }

    override fun sourceList(): List<ApiService> = Sources.values().toList()

    override fun toSource(s: String): ApiService? = try {
        Sources.valueOf(s)
    } catch (e: IllegalArgumentException) {
        null
    }

    override fun downloadChapter(chapterModel: ChapterModel, title: String) {

    }

    override val apkString: String get() = "novelworld-debug.apk"

    override val showMiddleChapterButton: Boolean get() = false

}