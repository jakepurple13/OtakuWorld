package com.programmersbox.mangaworld

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.downloadManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.manga_sources.Sources
import com.programmersbox.manga_sources.utilities.NetworkHelper
import com.programmersbox.models.*
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.SettingsDsl
import com.programmersbox.uiviews.utils.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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

    override fun chapterOnClick(model: ChapterModel, allChapters: List<ChapterModel>, context: Context) {
        context.startActivity(
            Intent(context, if (context.useNewReader) ReadActivityCompose::class.java else ReadActivity::class.java).apply {
                putExtra("currentChapter", model.toJson(ChapterModel::class.java to ChapterModelSerializer()))
                putExtra("allChapters", allChapters.toJson(ChapterModel::class.java to ChapterModelSerializer()))
                putExtra("mangaTitle", model.name)
                putExtra("mangaUrl", model.url)
                putExtra("mangaInfoUrl", model.sourceUrl)
            }
        )
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
                            "MangaWorld/$title/${model.name}/${String.format("%03d", index)}.png"
                        )
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setAllowedOverRoaming(true)
                        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                        .setMimeType("image/jpeg")
                        .setTitle(model.name)
                        .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) Gecko/20100101 Firefox/77")
                        .addRequestHeader("Accept-Language", "en-US,en;q=0.5")
                }
            }
            .subscribeBy { it.fastForEach(context.downloadManager::enqueue) }
            .addTo(disposable)
    }

    override fun downloadChapter(chapterModel: ChapterModel, title: String) {
        MainActivity.activity.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) { p ->
            if (p.isGranted) downloadFullChapter(chapterModel, title)
        }
    }

    override fun customPreferences(preferenceScreen: SettingsDsl) {
        preferenceScreen.generalSettings { fragment, it ->
            it.addPreference(
                SwitchPreference(it.context).apply {
                    title = it.context.getString(R.string.showAdultSources)
                    isChecked = context.showAdult
                    setOnPreferenceChangeListener { _, newValue ->
                        context.showAdult = newValue as Boolean
                        if (!newValue && sourcePublish.value == Sources.TSUMINO) {
                            sourcePublish.onNext(Sources.values().random())
                        }
                        true
                    }
                    icon = ContextCompat.getDrawable(it.context, R.drawable.ic_baseline_text_format_24)
                }
            )

            it.addPreference(
                SeekBarPreference(it.context).apply {
                    title = it.context.getString(R.string.reader_padding_between_pages)
                    summary = it.context.getString(R.string.default_padding_summary)
                    setOnPreferenceChangeListener { _, newValue ->
                        if (newValue is Int) {
                            fragment.lifecycleScope.launch(Dispatchers.IO) { it.context.updatePref(PAGE_PADDING, newValue) }
                        }
                        true
                    }
                    setDefaultValue(4)
                    showSeekBarValue = true
                    icon = ContextCompat.getDrawable(it.context, R.drawable.ic_baseline_format_line_spacing_24)
                    min = 0
                    max = 10
                    fragment.lifecycleScope.launch {
                        it.context.pagePadding
                            .flowWithLifecycle(fragment.lifecycle)
                            .collect { runOnUIThread { value = it } }
                    }
                }
            )

            it.addPreference(
                SwitchPreference(it.context).apply {
                    title = it.context.getString(R.string.useNewReader)
                    isChecked = context.useNewReader
                    setOnPreferenceChangeListener { _, newValue ->
                        context.useNewReader = newValue as Boolean
                        true
                    }
                    icon = ContextCompat.getDrawable(it.context, R.drawable.ic_baseline_chrome_reader_mode_24)
                }
            )
        }

        preferenceScreen.viewSettings { s, it ->
            it.addPreference(
                Preference(it.context).apply {
                    title = it.context.getString(R.string.downloaded_manga)
                    setOnPreferenceClickListener {
                        s.findNavController()
                            .navigate(DownloadViewerFragment::class.java.hashCode(), null, SettingsDsl.customAnimationOptions)
                        true
                    }
                    icon = ContextCompat.getDrawable(it.context, R.drawable.ic_baseline_library_books_24)
                }
            )
        }

        preferenceScreen.navigationSetup {
            it.findNavController()
                .graph
                .addDestination(
                    FragmentNavigator(it.requireContext(), it.childFragmentManager, R.id.setting_nav).createDestination().apply {
                        id = DownloadViewerFragment::class.java.hashCode()
                        className = DownloadViewerFragment::class.java.name
                    }
                )
        }
    }

    override fun sourceList(): List<ApiService> =
        if (context.showAdult) Sources.values().toList() else Sources.values().filterNot(Sources::isAdult).toList()

    override fun toSource(s: String): ApiService? = try {
        Sources.valueOf(s)
    } catch (e: IllegalArgumentException) {
        null
    }

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    @Composable
    override fun ComposeShimmerItem() {
        LazyVerticalGrid(cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH)) {
            items(10) { PlaceHolderCoverCard(placeHolder = R.drawable.manga_world_round_logo) }
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
        LazyVerticalGrid(
            cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
            state = listState
        ) {
            items(list.size) { i ->
                list.getOrNull(i)?.let {
                    CoverCard(
                        imageUrl = it.imageUrl,
                        name = it.title,
                        placeHolder = R.drawable.manga_world_round_logo,
                        favoriteIcon = {
                            if (favorites.fastAny { f -> f.url == it.url }) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                                Icon(
                                    Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.onPrimary,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                    ) { onClick(it) }
                }
            }
        }
    }

}