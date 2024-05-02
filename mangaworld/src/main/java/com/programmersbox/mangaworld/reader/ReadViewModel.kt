package com.programmersbox.mangaworld.reader

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.mangaworld.ChapterHolder
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.Storage
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.GenericInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import java.io.File

class ReadViewModel(
    mangaReader: MangaReader,
    private val dao: ItemDao,
    val genericInfo: GenericInfo,
    private val chapterHolder: ChapterHolder,
    val headers: MutableMap<String, String> = mutableMapOf(),
    model: Flow<List<String>>? = chapterHolder.chapterModel
        ?.getChapterInfo()
        ?.map {
            headers.putAll(it.flatMap { h -> h.headers.toList() })
            it.mapNotNull(Storage::link)
        },
    /*?.subscribeOn(Schedulers.io())
    ?.observeOn(AndroidSchedulers.mainThread())*/
    //?.doOnError { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show() },
    val isDownloaded: Boolean = mangaReader.downloaded,
    filePath: File? = mangaReader.filePath?.let { File(it) },
    modelPath: Flow<List<String>>? = if (isDownloaded && filePath != null) {
        flow {
            filePath
                .listFiles()
                ?.sortedBy { f -> f.name.split(".").first().toInt() }
                ?.fastMap(File::toUri)
                ?.fastMap(Uri::toString)
                ?.let { emit(it) } ?: throw Exception("Cannot find files")
        }
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)
    } else {
        model
    },
) : ViewModel(), KoinComponent {

    companion object {
        fun navigateToMangaReader(
            navController: NavController,
            mangaTitle: String? = null,
            mangaUrl: String? = null,
            mangaInfoUrl: String? = null,
            downloaded: Boolean = false,
            filePath: String? = null
        ) {
            navController.navigate(MangaReader(mangaTitle, mangaUrl, mangaInfoUrl, downloaded, filePath)) { launchSingleTop = true }
        }
    }

    @Serializable
    data class MangaReader(
        val mangaTitle: String? = null,
        val mangaUrl: String? = null,
        val mangaInfoUrl: String? = null,
        val downloaded: Boolean,
        val filePath: String? = null,
    )

    val title by lazy { mangaReader.mangaTitle ?: "" }

    var list by mutableStateOf<List<ChapterModel>>(emptyList())

    private val mangaUrl by lazy { mangaReader.mangaInfoUrl ?: "" }

    var currentChapter: Int by mutableIntStateOf(0)

    val pageList = mutableStateListOf<String>()
    var isLoadingPages by mutableStateOf(false)

    val currentChapterModel by derivedStateOf { list.getOrNull(currentChapter) }

    init {
        val url = chapterHolder.chapterModel?.url ?: mangaReader.mangaUrl
        list = chapterHolder.chapters.orEmpty()
        currentChapter = list.indexOfFirst { l -> l.url == url }.coerceIn(0, list.lastIndex)

        loadPages(modelPath)
    }

    var showInfo by mutableStateOf(true)

    var firstScroll by mutableStateOf(true)

    fun addChapterToWatched(newChapter: Int, chapter: () -> Unit) {
        currentChapter = newChapter
        list.getOrNull(newChapter)?.let { item ->
            ChapterWatched(item.url, item.name, mangaUrl)
                .let {
                    viewModelScope.launch {
                        dao.insertChapter(it)
                        FirebaseDb.insertEpisodeWatchedFlow(it).collect()
                        withContext(Dispatchers.Main) { chapter() }
                    }
                }

            item
                .getChapterInfo()
                .map { it.mapNotNull(Storage::link) }
                .let { loadPages(it) }
        }
    }

    private fun loadPages(modelPath: Flow<List<String>>?) {
        modelPath
            ?.onStart {
                isLoadingPages = true
                pageList.clear()
            }
            ?.onEach { pageList.addAll(it) }
            ?.onCompletion { isLoadingPages = false }
            ?.launchIn(viewModelScope)
    }

    fun refresh() {
        headers.clear()
        loadPages(
            list.getOrNull(currentChapter)
                ?.getChapterInfo()
                ?.map {
                    headers.putAll(it.flatMap { h -> h.headers.toList() })
                    it.mapNotNull(Storage::link)
                }
        )
    }

    override fun onCleared() {
        super.onCleared()
        chapterHolder.chapterModel = null
        chapterHolder.chapters = null
    }
}