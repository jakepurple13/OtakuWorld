package com.programmersbox.novelworld

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.placeholder.material.placeholder
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.gsonutils.getObject
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.defaultSharedPref
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.novel_sources.Sources
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.*
import org.koin.dsl.module

val appModule = module {
    single<GenericInfo> { GenericNovel(get()) }
    single { MainLogo(R.mipmap.ic_launcher) }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
}

class ChapterList(private val context: Context, private val genericInfo: GenericInfo) {
    fun set(item: List<ChapterModel>?) {
        val i = item.toJson(ChapterModel::class.java to ChapterModelSerializer())
        context.defaultSharedPref.edit().putString("chapterList", i).commit()
    }

    fun get(): List<ChapterModel>? = context.defaultSharedPref.getObject(
        "chapterList",
        null,
        ChapterModel::class.java to ChapterModelDeserializer(genericInfo)
    )
}

class GenericNovel(val context: Context) : GenericInfo {

    override val deepLinkUri: String get() = "novelworld"

    override fun chapterOnClick(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController
    ) {
        ChapterList(context, this@GenericNovel).set(allChapters)
        ReadViewModel.navigateToNovelReader(
            navController,
            model,
            model.name,
            model.url,
            model.sourceUrl
        )
    }

    override fun sourceList(): List<ApiService> = Sources.values().toList()

    override fun toSource(s: String): ApiService? = try {
        Sources.valueOf(s)
    } catch (e: IllegalArgumentException) {
        null
    }

    override fun downloadChapter(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity
    ) {
    }

    override val apkString: AppUpdate.AppUpdates.() -> String? get() = { novel_file }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    tonalElevation = 5.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(
                                true,
                                color = androidx.compose.material3
                                    .contentColorFor(backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                                    .copy(0.1f)
                                    .compositeOver(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                            )
                            .padding(5.dp)
                    )
                }
            }
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    @Composable
    override fun ItemListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(list) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                        .combineClickableWithIndication(
                            onLongPress = { c -> onLongPress(it, c) },
                            onClick = { onClick(it) }
                        ),
                    tonalElevation = 5.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    ListItem(
                        icon = {
                            Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        text = { androidx.compose.material3.Text(it.title) },
                        overlineText = { androidx.compose.material3.Text(it.source.serviceName) },
                        secondaryText = if (it.description.isNotEmpty()) {
                            { androidx.compose.material3.Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun NavGraphBuilder.navSetup() {
        composable(
            ReadViewModel.NovelReaderRoute,
            arguments = listOf(
                navArgument("currentChapter") { },
                navArgument("novelTitle") { },
                navArgument("novelUrl") { },
                navArgument("novelInfoUrl") { },
            )
        ) { NovelReader() }
    }

    override fun deepLinkDetails(context: Context, itemModel: ItemModel?): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            "${Screen.DetailsScreen.route}/${Uri.encode(itemModel.toJson(ApiService::class.java to ApiServiceSerializer()))}".toUri(),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(itemModel?.hashCode() ?: 0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun deepLinkSettings(context: Context): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Screen.NotificationScreen.route.toUri(),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(13, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}