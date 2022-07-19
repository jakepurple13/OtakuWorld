package com.programmersbox.mangaworld.downloads

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.mangaworld.ChaptersGet
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class DownloadViewModel(context: Context, defaultPathname: File) : ViewModel() {

    companion object {
        const val DownloadRoute = "downloads"
    }

    val disposable = CompositeDisposable()

    var fileList by mutableStateOf<Map<String, Map<String, List<ChaptersGet.Chapters>>>>(emptyMap())

    private val c = ChaptersGet.getInstance(context).also { c ->
        c?.loadChapters(viewModelScope, defaultPathname.absolutePath)
        viewModelScope.launch {
            c?.chapters2
                ?.map { f ->
                    f
                        .groupBy { it.folder }
                        .entries
                        .toList()
                        .fastMap { it.key to it.value.groupBy { c -> c.chapterFolder } }
                        .toMap()
                }
                ?.collect { fileList = it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        c?.unregister()
        disposable.dispose()
    }

}