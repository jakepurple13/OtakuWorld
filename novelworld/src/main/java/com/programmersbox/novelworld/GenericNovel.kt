package com.programmersbox.novelworld

import android.content.Context
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import com.google.accompanist.placeholder.material.placeholder
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.novel_sources.Sources
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ChapterModelSerializer
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.animatedItems
import com.programmersbox.uiviews.utils.updateAnimatedItemsState
import org.koin.dsl.module

val appModule = module {
    single<GenericInfo> { GenericNovel(get()) }
    single { MainLogo(R.mipmap.ic_launcher) }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
}

class GenericNovel(val context: Context) : GenericInfo {

    override fun chapterOnClick(model: ChapterModel, allChapters: List<ChapterModel>, infoModel: InfoModel, context: Context) {
        context.startActivity(
            Intent(context, ReadingActivity::class.java).apply {
                putExtra("model", model.toJson(ChapterModel::class.java to ChapterModelSerializer()))
                putExtra("allChapters", allChapters.toJson(ChapterModel::class.java to ChapterModelSerializer()))
                putExtra("novelTitle", model.name)
                putExtra("novelUrl", model.url)
                putExtra("novelInfoUrl", model.sourceUrl)
            }
        )
    }

    override fun sourceList(): List<ApiService> = Sources.values().toList()

    override fun toSource(s: String): ApiService? = try {
        Sources.valueOf(s)
    } catch (e: IllegalArgumentException) {
        null
    }

    override fun downloadChapter(chapterModel: ChapterModel, title: String) {}

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

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    override fun ItemListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyListState,
        onClick: (ItemModel) -> Unit
    ) {
        val animated by updateAnimatedItemsState(newList = list)
        LazyColumn(state = listState) {
            animatedItems(
                animated,
                enterTransition = fadeIn(),
                exitTransition = fadeOut()
            ) {
                Card(
                    onClick = { onClick(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    elevation = 5.dp
                ) {
                    ListItem(
                        icon = {
                            Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        text = { Text(it.title) },
                        overlineText = { Text(it.source.serviceName) },
                        secondaryText = if (it.description.isNotEmpty()) {
                            { Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }

}