package com.programmersbox.mangaworld.downloads

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.mangaworld.ChaptersGet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data object DownloadRoute : NavKey

class DownloadViewModel(context: Context, defaultPathname: File) : ViewModel() {

    val fileList = mutableStateMapOf<String, Map<String, List<ChaptersGet.Chapters>>>()

    private val c = ChaptersGet.getInstance(context).also { c ->
        c?.loadChapters(viewModelScope, defaultPathname.absolutePath)
        c?.chapters
            ?.map { f ->
                f
                    .groupBy { it.folder }
                    .entries
                    .toList()
                    .fastMap { it.key to it.value.groupBy { c -> c.chapterFolder } }
                    .toMap()
            }
            ?.onEach {
                fileList.clear()
                fileList.putAll(it)
            }
            ?.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        c?.unregister()
    }

}