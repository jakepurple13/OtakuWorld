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
data class KmpSubtitle(
    val url: String,
    val lang: String,
    val headers: Map<String, String> = emptyMap(),
)

/** Some sources leak JSON escapes (`\/`, `\"`, `\\`, `\u00XX`) into subtitle labels; undo them and trim. */
fun cleanSubtitleLabel(label: String): String {
    val out = StringBuilder(label.length)
    var i = 0
    while (i < label.length) {
        val c = label[i]
        val next = label.getOrNull(i + 1)
        val hex = if (next == 'u' && i + 6 <= label.length) label.substring(i + 2, i + 6) else null
        when {
            c != '\\' || next == null -> out.append(c)
            next == '/' || next == '"' || next == '\\' -> {
                out.append(next)
                i++
            }

            hex != null && hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' } -> {
                out.append(hex.toInt(16).toChar())
                i += 5
            }

            else -> out.append(c)
        }
        i++
    }
    return out.toString().trim()
}

@kotlinx.serialization.Serializable
data class KmpStorage(
    var sub: String? = null,
    var source: String? = null,
    var link: String? = null,
    var quality: String? = null,
    var filename: String? = null,
    var subtitles: List<KmpSubtitle> = emptyList(),
    var server: String? = null,
    /** One of "application/x-mpegURL", "application/dash+xml", "video/mp4"; null = infer. */
    var mimeType: String? = null,
) {
    /**
     * Body property: excluded from `equals`, `copy()` and serialization. Copy it by hand.
     */
    val headers = mutableMapOf<String, String>()

    /**
     * Source-private keys (e.g. a lazy hoster id).
     *
     * Body property: excluded from `equals`, `copy()` and serialization. Copy it by hand.
     */
    val extras = mutableMapOf<String, String>()
}

//TODO: Make a new module (KmpModels), leaving this one alone.
// That module will be the generic one that kmpuiviews/UIViews will use.
// The new module will also contain a mapping function