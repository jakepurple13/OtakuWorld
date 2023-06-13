package com.programmersbox.uiviews.utils

import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.lightColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import androidx.compose.material.MaterialTheme as M2MaterialTheme

val MockInfo = object : GenericInfo {
    override val apkString: AppUpdate.AppUpdates.() -> String? = { "" }
    override val deepLinkUri: String = ""
    override fun deepLinkDetails(context: Context, itemModel: ItemModel?): PendingIntent? = null
    override fun deepLinkSettings(context: Context): PendingIntent? = null
    override fun chapterOnClick(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController
    ) {

    }

    override fun sourceList(): List<ApiService> = emptyList()

    override fun toSource(s: String): ApiService? = null

    override fun downloadChapter(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController
    ) {

    }

    @Composable
    override fun ComposeShimmerItem() {
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) { items(10) { M3PlaceHolderCoverCard(placeHolder = R.drawable.github_icon) } }
    }

    @ExperimentalFoundationApi
    @Composable
    override fun ItemListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    ) {
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                list,
                key = { i, it -> "${it.url}$i" },
                contentType = { _, i -> i }
            ) { _, it ->
                M3CoverCard(
                    onLongPress = { c -> onLongPress(it, c) },
                    imageUrl = it.imageUrl,
                    name = it.title,
                    headers = it.extras,
                    placeHolder = R.drawable.github_icon,
                    favoriteIcon = {
                        if (favorites.any { f -> f.url == it.url }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }
                    }
                ) { onClick(it) }
            }
        }
    }
}

@Composable
fun PreviewTheme(
    navController: NavHostController = rememberNavController(),
    genericInfo: GenericInfo = MockInfo,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
            darkTheme -> darkColorScheme(
                primary = Color(0xff90CAF9),
                secondary = Color(0xff90CAF9)
            )

            else -> lightColorScheme(
                primary = Color(0xff2196F3),
                secondary = Color(0xff90CAF9)
            )
        }
    ) {
        M2MaterialTheme(
            colors = if (darkTheme)
                darkColors(
                    primary = Color(0xff90CAF9),
                    secondary = Color(0xff90CAF9)
                )
            else
                lightColors(
                    primary = Color(0xff2196F3),
                    secondary = Color(0xff90CAF9)
                ),
        ) {
            val systemUiController = rememberSystemUiController()

            SideEffect {
                systemUiController.setNavigationBarColor(
                    color = Color.Transparent,
                    darkIcons = !darkTheme
                )
                systemUiController.setStatusBarColor(
                    color = Color.Transparent,
                    darkIcons = !darkTheme
                )
            }

            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalGenericInfo provides genericInfo,
                LocalSettingsHandling provides remember { SettingsHandling(context) },
                LocalActivity provides remember {
                    object : BaseMainActivity() {
                        override fun onCreate() {}
                    }
                },
                LocalItemDao provides remember { ItemDatabase.getInstance(context).itemDao() },
                LocalHistoryDao provides remember { HistoryDatabase.getInstance(context).historyDao() },
                LocalCustomListDao provides remember { ListDatabase.getInstance(context).listDao() },
            ) { content() }
        }
    }
}

val MockAppIcon = MainLogo(R.drawable.github_icon)

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, group = "themes")
@Preview(showBackground = true, group = "themes")
internal annotation class LightAndDarkPreviews