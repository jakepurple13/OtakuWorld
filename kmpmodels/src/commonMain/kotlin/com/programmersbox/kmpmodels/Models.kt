package com.programmersbox.kmpmodels

data class KmpItemModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: KmpApiService,
) {
    val extras = mutableMapOf<String, Any>()
    val otherExtras = mutableMapOf<String, Any>()
    fun toInfoModel() = source.getItemInfoFlow(this)
}

data class KmpInfoModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val chapters: List<KmpChapterModel>,
    val genres: List<String>,
    val alternativeNames: List<String>,
    val source: KmpApiService,
) {
    val extras = mutableMapOf<String, Any>()
}

data class KmpChapterModel(
    val name: String,
    val url: String,
    val uploaded: String,
    val sourceUrl: String,
    val source: KmpApiService,
) {
    var uploadedTime: Long? = null
    fun getChapterInfo() = source.getChapterInfoFlow(this)
    val extras = mutableMapOf<String, Any>()
    val otherExtras = mutableMapOf<String, Any>()
}

class NormalLink(var normal: Normal? = null)
class Normal(var storage: Array<KmpStorage>? = emptyArray())

@kotlinx.serialization.Serializable
data class KmpStorage(
    var sub: String? = null,
    var source: String? = null,
    var link: String? = null,
    var quality: String? = null,
    var filename: String? = null,
) {
    val headers = mutableMapOf<String, String>()
}

//TODO: Make a new module (KmpModels), leaving this one alone.
// That module will be the generic one that kmpuiviews/UIViews will use.
// The new module will also contain a mapping function