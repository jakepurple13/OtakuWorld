package com.programmersbox.animeworld.videochoice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.Storage

data class VideoSourceModel(
    val c: List<Storage>,
    val infoModel: InfoModel,
    val isStreaming: Boolean,
    val model: ChapterModel,
) {
    companion object {
        var showVideoSources by mutableStateOf<VideoSourceModel?>(null)
    }
}