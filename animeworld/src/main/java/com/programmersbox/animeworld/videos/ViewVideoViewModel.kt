package com.programmersbox.animeworld.videos

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.animeworld.VideoContent
import com.programmersbox.animeworld.VideoGet
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object VideoViewerRoute : NavKey

class ViewVideoViewModel(context: Context) : ViewModel() {

    var videos by mutableStateOf<List<VideoContent>>(emptyList())

    private val v = VideoGet.getInstance(context).also { v ->
        v?.loadVideos(viewModelScope, VideoGet.externalContentUri)
        viewModelScope.launch { v?.videos2?.collect { videos = it } }
    }

    override fun onCleared() {
        super.onCleared()
        v?.unregister()
    }
}