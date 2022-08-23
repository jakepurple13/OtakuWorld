package com.programmersbox.animeworld.videochoice

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.Storage
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ApiServiceDeserializer
import com.programmersbox.uiviews.utils.ApiServiceSerializer

class VideoSourceViewModel(
    handle: SavedStateHandle,
    genericInfo: GenericInfo
) : ViewModel() {
    val items = handle.get<String>("storage").fromJson<List<Storage>>().orEmpty()
    val name = handle.get<String>("name").orEmpty()
    val isStream = handle.get<String>("isStreaming")?.toBoolean() ?: false
    val infoModel = handle.get<String>("infoModel")
        .fromJson<InfoModel>(ApiService::class.java to ApiServiceDeserializer(genericInfo))
    val model = handle.get<String>("chapterModel")
        .fromJson<ChapterModel>(ApiService::class.java to ApiServiceDeserializer(genericInfo))

    companion object {
        const val route = "videosource/storage={storage}&name={name}&isStreaming={isStreaming}&infoModel={infoModel}&chapterModel={chapterModel}"

        fun createNavigationRoute(
            c: List<Storage>,
            infoModel: InfoModel,
            isStreaming: Boolean,
            model: ChapterModel
        ): String {
            fun <T> T.toUriJson() = Uri.encode(toJson())
            fun <T> T.toUriModelJson() = Uri.encode(toJson(ApiService::class.java to ApiServiceSerializer()))
            return "videosource/storage=${c.toUriJson()}&name=${model.name}&isStreaming=$isStreaming&infoModel=${infoModel.toUriModelJson()}&chapterModel=${model.toUriModelJson()}"
        }
    }
}