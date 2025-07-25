package com.programmersbox.uiviews.utils

import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.di.databases
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard
import com.programmersbox.kmpuiviews.presentation.components.placeholder.M3PlaceHolderCoverCard
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.navactions.TopLevelBackStack
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.KmpLocalCompositionSetup
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSettingsHandling
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.sharedutils.AppLogo
import com.programmersbox.sharedutils.FirebaseUIStyle
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.datastore.OtakuDataStoreHandling
import com.programmersbox.uiviews.datastore.SettingsHandling
import com.programmersbox.uiviews.di.androidViewModels
import com.programmersbox.uiviews.di.appModules
import com.programmersbox.uiviews.di.kmpInterop
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.KoinApplication
import org.koin.dsl.binds
import org.koin.dsl.module

class MockInfo(private val context: Context) : GenericInfo {
    override val apkString: AppUpdate.AppUpdates.() -> String? = { "" }
    override val deepLinkUri: String = ""
    override fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent? = null
    override fun deepLinkSettings(context: Context): PendingIntent? = null
    override fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {

    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
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
        ) { items(10) { M3PlaceHolderCoverCard(placeHolder = painterResource(R.drawable.ic_site_settings)) } }
    }

    @ExperimentalFoundationApi
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
            columns = adaptiveGridCell(),
            state = listState,
            contentPadding = paddingValues,
            modifier = modifier.fillMaxSize(),
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
                    placeHolder = { painterResource(R.drawable.ic_site_settings) },
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

val MockApiService = object : KmpApiService {
    override val baseUrl: String = ""
}

class AmoledProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> =
        sequenceOf(true, false)
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun PreviewTheme(
    navController: NavHostController = rememberNavController(),
    genericInfo: GenericInfo = MockInfo(LocalContext.current),
    isAmoledMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    KoinApplication(
        application = {
            androidLogger()
            androidContext(context)
            modules(
                appModules,
                androidViewModels,
                databases,
                kmpInterop,
                module {
                    single { FirebaseUIStyle(R.style.Theme_OtakuWorldBase) }
                    single { SettingsHandling(context, PerformanceClass.create()) }
                    single { AppLogo(AppCompatResources.getDrawable(context, R.drawable.ic_site_settings)!!, R.drawable.ic_site_settings) }
                },
                module {
                    single<GenericInfo> { MockInfo(get()) } binds arrayOf(
                        KmpGenericInfo::class,
                        GenericInfo::class
                    )
                    single { OtakuDataStoreHandling() }
                }
            )
        }
    ) {
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
            }.let {
                if (isAmoledMode && darkTheme) {
                    it.copy(
                        surface = Color.Black,
                        inverseSurface = Color.White,
                        background = Color.Black
                    )
                } else {
                    it
                }
            }
        ) {
            CompositionLocalProvider(
                LocalSettingsHandling provides remember {
                    NewSettingsHandling(
                        createProtobuf(context, SettingsSerializer()),
                    )
                },
                LocalNavHostPadding provides PaddingValues(0.dp),
                LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(1000.dp, 1000.dp)),
                //LocalSystemDateTimeFormat provides DateTimeFormatItem(isUsing24HourTime = DateTimeFormatHandler(LocalContext.current).is24Time())
            ) {
                KmpLocalCompositionSetup(
                    navController,
                    remember { TopLevelBackStack(Screen.RecentScreen) }
                ) {
                    Surface { content() }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, group = "themes")
@Preview(showBackground = true, group = "themes")
annotation class LightAndDarkPreviews

@PreviewThemeColorsSizes
@Composable
private fun RecentPreview() {
    PreviewTheme {
        Text("Hello")
    }
}

@Composable
fun StandalonePreviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoledMode: Boolean = false,
    content: @Composable () -> Unit,
) {
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
        }.let {
            if (isAmoledMode && darkTheme) {
                it.copy(
                    surface = Color.Black,
                    inverseSurface = Color.White,
                    background = Color.Black
                )
            } else {
                it
            }
        },
    ) {
        Surface {
            content()
        }
    }
}