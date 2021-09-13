package com.programmersbox.novelworld

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.accompanist.placeholder.material.placeholder
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.ItemModel
import com.programmersbox.novel_sources.Sources
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.BaseListFragment
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.ItemListAdapter
import com.programmersbox.uiviews.utils.ChapterModelSerializer
import com.programmersbox.uiviews.utils.NotificationLogo
import org.koin.dsl.module

val appModule = module {
    single<GenericInfo> { GenericNovel(get()) }
    single { MainLogo(R.mipmap.ic_launcher) }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
}

class GenericNovel(val context: Context) : GenericInfo {
    override fun createAdapter(context: Context, baseListFragment: BaseListFragment): ItemListAdapter<RecyclerView.ViewHolder> =
        (NovelAdapter(context, baseListFragment) as ItemListAdapter<RecyclerView.ViewHolder>)

    override fun createLayoutManager(context: Context): RecyclerView.LayoutManager = LinearLayoutManager(context)

    override fun chapterOnClick(model: ChapterModel, allChapters: List<ChapterModel>, context: Context) {
        context.startActivity(
            Intent(context, ReadingActivity::class.java).apply {
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

    override val apkString: AppUpdate.AppUpdates.() -> String? get() = { novel_file }

    @ExperimentalMaterialApi
    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                ) {
                    Text(
                        "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(true)
                            .padding(5.dp)
                    )
                }
            }
        }
    }

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    override fun ItemListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyListState,
        onClick: (ItemModel) -> Unit
    ) {
        LazyColumn(state = listState) {
            items(list) { ListItem(text = { Text(it.title) }) }
        }
    }

}