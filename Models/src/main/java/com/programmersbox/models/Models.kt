package com.programmersbox.models

import java.io.Serializable

data class ItemModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: ApiService
) : Serializable {
    val extras = mutableMapOf<String, Any>()
    fun toInfoModel() = source.getItemInfoFlow(this)
}

data class InfoModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val chapters: List<ChapterModel>,
    val genres: List<String>,
    val alternativeNames: List<String>,
    val source: ApiService
) {
    val extras = mutableMapOf<String, Any>()
}

data class ChapterModel(
    val name: String,
    val url: String,
    val uploaded: String,
    val sourceUrl: String,
    val source: ApiService
) : Serializable {
    var uploadedTime: Long? = null
    fun getChapterInfo() = source.getChapterInfoFlow(this)
    val extras = mutableMapOf<String, Any>()
}

class NormalLink(var normal: Normal? = null)
class Normal(var storage: Array<Storage>? = emptyArray())
@kotlinx.serialization.Serializable
data class Storage(
    var sub: String? = null,
    var source: String? = null,
    var link: String? = null,
    var quality: String? = null,
    var filename: String? = null
) {
    val headers = mutableMapOf<String, String>()
}
