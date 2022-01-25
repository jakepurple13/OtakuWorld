package com.programmersbox.mangaworld

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyGridState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.downloadManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.manga_sources.Sources
import com.programmersbox.manga_sources.utilities.NetworkHelper
import com.programmersbox.models.*
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.ComposeSettingsDsl
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.SettingsDsl
import com.programmersbox.uiviews.utils.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import java.io.File

val appModule = module {
    single<GenericInfo> { GenericManga(get()) }
    single { NetworkHelper(get()) }
    single { MainLogo(R.mipmap.ic_launcher) }
    single { NotificationLogo(R.drawable.manga_world_round_logo) }
}

class GenericManga(val context: Context) : GenericInfo {

    private val disposable = CompositeDisposable()

    override val apkString: AppUpdate.AppUpdates.() -> String? get() = { manga_file }
    override val scrollBuffer: Int = 4

    override fun chapterOnClick(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        navController: NavController
    ) {
        if (runBlocking { context.useNewReaderFlow.first() }) {
            navController
                .navigate(
                    ReadActivityComposeFragment::class.java.hashCode(),
                    Bundle().apply {
                        putString("currentChapter", model.toJson(ChapterModel::class.java to ChapterModelSerializer()))
                        putString("allChapters", allChapters.toJson(ChapterModel::class.java to ChapterModelSerializer()))
                        putString("mangaTitle", infoModel.title)
                        putString("mangaUrl", model.url)
                        putString("mangaInfoUrl", model.sourceUrl)
                    },
                    SettingsDsl.customAnimationOptions
                )
        } else {
            context.startActivity(
                Intent(context, ReadActivity::class.java).apply {
                    putExtra("currentChapter", model.toJson(ChapterModel::class.java to ChapterModelSerializer()))
                    putExtra("allChapters", allChapters.toJson(ChapterModel::class.java to ChapterModelSerializer()))
                    putExtra("mangaTitle", infoModel.title)
                    putExtra("mangaUrl", model.url)
                    putExtra("mangaInfoUrl", model.sourceUrl)
                }
            )
        }
    }

    private fun downloadFullChapter(model: ChapterModel, title: String) {
        val fileLocation = DOWNLOAD_FILE_PATH

        val direct = File("$fileLocation$title/${model.name}/")
        if (!direct.exists()) direct.mkdir()

        model.getChapterInfo()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { it.mapNotNull(Storage::link) }
            .map {
                it.mapIndexed { index, s ->
                    DownloadManager.Request(s.toUri())
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            "MangaWorld/$title/${model.name}/${String.format("%03d", index)}"
                        )
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setAllowedOverRoaming(true)
                        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                        .setMimeType("image/*")
                        .setTitle(model.name)
                        .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) Gecko/20100101 Firefox/77")
                        .addRequestHeader("Accept-Language", "en-US,en;q=0.5")
                }
            }
            .subscribeBy { it.fastForEach(context.downloadManager::enqueue) }
            .addTo(disposable)
    }

    override fun downloadChapter(model: ChapterModel, allChapters: List<ChapterModel>, infoModel: InfoModel, context: Context) {
        MainActivity.activity.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) { p ->
            if (p.isGranted) downloadFullChapter(model, infoModel.title)
        }
    }

    override fun sourceList(): List<ApiService> =
        if (runBlocking { context.showAdultFlow.first() }) Sources.values().toList() else Sources.values().filterNot(Sources::isAdult).toList()

    override fun toSource(s: String): ApiService? = try {
        Sources.valueOf(s)
    } catch (e: IllegalArgumentException) {
        null
    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class
    )
    @Composable
    override fun ComposeShimmerItem() {
        LazyVerticalGrid(
            cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) { items(10) { M3PlaceHolderCoverCard(placeHolder = R.drawable.manga_world_round_logo) } }
    }

    @OptIn(
        ExperimentalMaterialApi::class,
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
        //TODO: See if you can modify this to perform better
        LazyVerticalGrid(
            cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            state = listState
        ) {
            itemsIndexed(list, key = { i, it -> "${it.url}$i" }) { _, it ->
                M3CoverCard(
                    onLongPress = { c -> onLongPress(it, c) },
                    imageUrl = it.imageUrl,
                    name = it.title,
                    placeHolder = R.drawable.manga_world_round_logo,
                    favoriteIcon = {
                        if (favorites.fastAny { f -> f.url == it.url }) {
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

    @OptIn(ExperimentalMaterialApi::class)
    override fun composeCustomPreferences(navController: NavController): ComposeSettingsDsl.() -> Unit = {

        viewSettings {
            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.downloaded_manga)) },
                settingIcon = { Icon(Icons.Default.LibraryBooks, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { navController.navigate(DownloadViewerFragment::class.java.hashCode(), null, SettingsDsl.customAnimationOptions) }
            )
        }

        generalSettings {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val showAdult by context.showAdultFlow.collectAsState(false)

            SwitchSetting(
                settingTitle = { Text(stringResource(R.string.showAdultSources)) },
                value = showAdult,
                settingIcon = { Icon(Icons.Default.TextFormat, null, modifier = Modifier.fillMaxSize()) },
                updateValue = {
                    scope.launch { context.updatePref(SHOW_ADULT, it) }
                    if (!it && (sourcePublish.value as? Sources)?.isAdult == true) {
                        sourcePublish.onNext(sourceList().random())
                    }
                }
            )
        }

        playerSettings {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            var padding by remember { mutableStateOf(runBlocking { context.pagePadding.first().toFloat() }) }

            SliderSetting(
                sliderValue = padding,
                settingTitle = { Text(stringResource(R.string.reader_padding_between_pages)) },
                settingSummary = { Text(stringResource(R.string.default_padding_summary)) },
                settingIcon = { Icon(Icons.Default.FormatLineSpacing, null) },
                range = 0f..10f,
                updateValue = { padding = it },
                onValueChangedFinished = { scope.launch { context.updatePref(BATTERY_PERCENT, padding.toInt()) } }
            )

            val reader by context.useNewReaderFlow.collectAsState(true)

            SwitchSetting(
                settingTitle = { Text(stringResource(R.string.useNewReader)) },
                summaryValue = { Text(stringResource(R.string.reader_summary_setting)) },
                settingIcon = { Icon(Icons.Default.ChromeReaderMode, null, modifier = Modifier.fillMaxSize()) },
                value = reader,
                updateValue = { scope.launch { context.updatePref(USER_NEW_READER, it) } }
            )
        }

    }

    private fun readerNavSetup(fragment: Fragment, navController: NavController, navId: Int) {
        navController
            .graph
            .addDestination(
                FragmentNavigator(fragment.requireContext(), fragment.childFragmentManager, navId).createDestination().apply {
                    id = ReadActivityComposeFragment::class.java.hashCode()
                    setClassName(ReadActivityComposeFragment::class.java.name)
                }
            )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            showOrHideNav.onNext(destination.id != ReadActivityComposeFragment::class.java.hashCode())
        }
    }

    override fun recentNavSetup(fragment: Fragment, navController: NavController) {
        readerNavSetup(fragment, navController, R.id.recent_nav)
    }

    override fun allNavSetup(fragment: Fragment, navController: NavController) {
        readerNavSetup(fragment, navController, R.id.all_nav)
    }

    override fun settingNavSetup(fragment: Fragment, navController: NavController) {
        readerNavSetup(fragment, navController, R.id.setting_nav)

        navController
            .graph
            .addDestination(
                FragmentNavigator(fragment.requireContext(), fragment.childFragmentManager, R.id.setting_nav).createDestination().apply {
                    id = DownloadViewerFragment::class.java.hashCode()
                    setClassName(DownloadViewerFragment::class.java.name)
                }
            )
    }

}