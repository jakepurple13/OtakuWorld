package com.programmersbox.animeworld

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.programmersbox.anime_sources.Sources
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.ItemListAdapter
import com.programmersbox.uiviews.utils.currentService
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class MainActivity : BaseMainActivity() {

    override fun onCreate() {

        if (currentService == null) {
            sourcePublish.onNext(Sources.GOGOANIME)
            currentService = Sources.GOGOANIME
        }

        //TODO: For view videos and view downloads, use a bottomsheetdialogfragment!

    }

    override fun createAdapter(context: Context, baseListFragment: BaseListFragment): ItemListAdapter<RecyclerView.ViewHolder> =
        (AnimeAdapter(context, baseListFragment) as ItemListAdapter<RecyclerView.ViewHolder>)

    override fun createLayoutManager(context: Context): RecyclerView.LayoutManager = LinearLayoutManager(context)

    override fun chapterOnClick(model: ChapterModel, allChapters: List<ChapterModel>, context: Context) {

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

    override fun customPreferences(preferenceScreen: PreferenceScreen) {
        preferenceScreen.addPreference(Preference(preferenceScreen.context).apply { title = "Hello" })
    }

}
