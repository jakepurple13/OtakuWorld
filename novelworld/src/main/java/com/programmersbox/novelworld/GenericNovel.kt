package com.programmersbox.novelworld

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.gsonutils.getObject
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.defaultSharedPref
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.uiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.uiviews.presentation.components.placeholder.shimmer
import com.programmersbox.uiviews.utils.ChapterModelDeserializer
import com.programmersbox.uiviews.utils.ChapterModelSerializer
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.combineClickableWithIndication
import com.programmersbox.uiviews.utils.trackScreen
import org.koin.dsl.module

val appModule = module {
    single<GenericInfo> { GenericNovel(get()) }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
}

class ChapterList(private val context: Context, private val genericInfo: GenericInfo) {
    fun set(item: List<KmpChapterModel>?) {
        val i = item.toJson(KmpChapterModel::class.java to ChapterModelSerializer())
        context.defaultSharedPref.edit().putString("chapterList", i).commit()
    }

    fun get(): List<KmpChapterModel>? = context.defaultSharedPref.getObject(
        "chapterList",
        null,
        KmpChapterModel::class.java to ChapterModelDeserializer()
    )
}

class GenericNovel(val context: Context) : GenericInfo {

    override val deepLinkUri: String get() = "novelworld://"

    override val sourceType: String get() = "novel"

    override fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController,
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

    override fun sourceList(): List<KmpApiService> = emptyList()

    override fun toSource(s: String): KmpApiService? = null

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController,
    ) {

    }

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (BuildConfig.FLAVOR) {
                "noFirebase" -> novelNoFirebaseFile
                "noCloudFirebase" -> novelNoCloudFile
                else -> novelFile
            }
        }

    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .m3placeholder(
                                true,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                            .padding(4.dp)
                    )
                }
            }
        }
    }

    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class
    )
    @Composable
    override fun ItemListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = paddingValues,
            modifier = modifier.fillMaxSize(),
        ) {
            items(list) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .combineClickableWithIndication(
                            onLongPress = { c -> onLongPress(it, c) },
                            onClick = { onClick(it) }
                        ),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(it.title) },
                        overlineContent = { Text(it.source.serviceName) },
                        supportingContent = if (it.description.isNotEmpty()) {
                            { Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun NavGraphBuilder.globalNavSetup() {
        composable(
            ReadViewModel.NovelReaderRoute,
            arguments = listOf(
                navArgument("currentChapter") { },
                navArgument("novelTitle") { },
                navArgument("novelUrl") { },
                navArgument("novelInfoUrl") { },
            )
        ) {
            trackScreen("novelReader")
            NovelReader()
        }
    }

    override fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkDetailsUri(itemModel),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(itemModel?.hashCode() ?: 0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun deepLinkSettings(context: Context): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkSettingsUri(),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(13, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}