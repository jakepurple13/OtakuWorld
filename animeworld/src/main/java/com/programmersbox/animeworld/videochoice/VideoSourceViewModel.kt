package com.programmersbox.animeworld.videochoice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage

data class VideoSourceModel(
    val c: List<KmpStorage>,
    val infoModel: KmpInfoModel,
    val isStreaming: Boolean,
    val model: KmpChapterModel,
) {
    companion object {
        var showVideoSources by mutableStateOf<VideoSourceModel?>(null)
    }
}