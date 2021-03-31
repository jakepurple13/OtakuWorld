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
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.ItemListAdapter
import com.programmersbox.uiviews.utils.currentService

class MainActivity : BaseMainActivity() {

    override fun createGenericInfo(): GenericInfo = object : GenericInfo {

        override fun createAdapter(context: Context, baseListFragment: BaseListFragment): ItemListAdapter<RecyclerView.ViewHolder> =
            (AnimeAdapter(context, baseListFragment) as ItemListAdapter<RecyclerView.ViewHolder>)

        override fun createLayoutManager(context: Context): RecyclerView.LayoutManager = LinearLayoutManager(context)

        override fun chapterOnClick(model: ChapterModel, context: Context) {

        }

        override fun sourceList(): List<ApiService> = Sources.values().toList()

        override fun toSource(s: String): ApiService? = try {
            Sources.valueOf(s)
        } catch (e: IllegalArgumentException) {
            null
        }

    }

    override fun onCreate() {

        if(currentService == null) {
            sourcePublish.onNext(Sources.GOGOANIME)
            currentService = Sources.GOGOANIME
        }

    }

}
