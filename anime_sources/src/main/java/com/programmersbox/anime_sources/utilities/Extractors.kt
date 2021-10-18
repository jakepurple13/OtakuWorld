package com.programmersbox.anime_sources.utilities

import androidx.compose.ui.util.fastMap
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.header
import com.programmersbox.models.Storage
import org.jsoup.Jsoup
import java.lang.Thread.sleep

val extractors: List<Extractor> = listOf(
    XStreamCdn,
    StreamSB,
    Mp4Upload,
    MixDrop,
    DoodSoExtractor,
    DoodToExtractor,
    DoodLaExtractor()
)

interface Extractor {
    val name: String
    val mainUrl: String
    fun getUrl(url: String): List<Storage>
}

object XStreamCdn : Extractor {
    override val name: String = "XStreamCdn"
    override val mainUrl: String = "https://embedsito.com"
    val requiresReferer = false

    private data class ResponseData(
        val file: String,
        val label: String,
        //val type: String // Mp4
    )

    private data class ResponseJson(
        val success: Boolean,
        val data: List<ResponseData>?
    )

    fun getExtractorUrl(id: String): String = "$mainUrl/api/source/$id"

    private fun getQuality(string: String) = when (string) {
        "360p" -> Qualities.P480
        "480p" -> Qualities.P480
        "720p" -> Qualities.P720
        "1080p" -> Qualities.P1080
        else -> Qualities.Unknown
    }

    override fun getUrl(url: String): List<Storage> {
        val headers = mapOf(
            "Referer" to url,
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0",
        )
        val newUrl = url.replace("$mainUrl/v/", "$mainUrl/api/source/")
        //val extractedLinksList: MutableList<ExtractorLink> = mutableListOf()
        //println(Jsoup.connect(newUrl).headers(headers).post())
        //println(com.programmersbox.gsonutils.getApi(newUrl) { header(*headers.toList().toTypedArray()) })
        //println(newUrl)
        //println(url)
        val response = postApiMethod(newUrl) { header(*headers.toList().toTypedArray()) }.let {
            if (it is ApiResponse.Success) {
                it.body.fromJson<ResponseJson>()?.data?.fastMap {
                    Storage(
                        link = it.file,
                        source = url,
                        filename = name,
                        quality = name + " - " + it.label,
                        sub = getQuality(it.label).value.toString()
                    )
                }
            } else {
                emptyList()
            }
        }.orEmpty()
        //println(response)
        //return emptyList()
        return response
        /*return with(getJsonApi<ResponseJson>(newUrl) { header(*headers.toList().toTypedArray()) }) {
            this?.data?.fastMap {
                Storage(
                    link = it.file,
                    source = url,
                    filename = name,
                    quality = it.label,
                    sub = getQuality(it.label).value.toString()
                )
            }
        }
            .orEmpty()*/
    }
}

private val packedRegex = Regex("""eval\(function\(p,a,c,k,e,.*\)\)""")
fun getPacked(string: String): String? {
    return packedRegex.find(string)?.value
}

fun getAndUnpack(string: String): String {
    val packedText = getPacked(string)
    return JsUnpacker(packedText).unpack() ?: string
}

object StreamSB : Extractor {
    override val name: String = "StreamSB"
    override val mainUrl: String = "https://sbplay.org"
    private val sourceRegex = Regex("""sources:[\W\w]*?file:\s*"(.*?)"""")

    //private val m3u8Regex = Regex(""".*?(\d*).m3u8""")
    //private val urlRegex = Regex("""(.*?)([^/]+$)""")

    // 1: Resolution 2: url
    private val m3u8UrlRegex = Regex("""RESOLUTION=\d*x(\d*).*\n(http.*.m3u8)""")
    val requiresReferer = false

    // 	https://sbembed.com/embed-ns50b0cukf9j.html   ->   https://sbvideo.net/play/ns50b0cukf9j
    override fun getUrl(url: String): List<Storage> {
        //val extractedLinksList: MutableList<ExtractorLink> = mutableListOf()
        val newUrl = url.replace("sbplay.org/embed-", "sbplay.org/play/").removeSuffix(".html")
        return com.programmersbox.gsonutils.getApi(newUrl)?.let {
            println(it)
            sourceRegex.findAll(getAndUnpack(it)).map { sourceMatch ->
                val extractedUrl = sourceMatch.groupValues[1]
                if (extractedUrl.contains(".m3u8")) {
                    with(com.programmersbox.gsonutils.getApi(extractedUrl)) {
                        println(this)
                        m3u8UrlRegex.findAll(this.toString())
                            .map { match ->
                                val extractedUrlM3u8 = match.groupValues[2]
                                val extractedRes = match.groupValues[1]
                                Storage(
                                    link = extractedUrlM3u8,
                                    source = extractedUrl,
                                    filename = name,
                                    quality = "$name - $extractedRes",
                                    sub = getQualityFromName(extractedRes).value.toString()
                                )
                                /*extractedLinksList.add(
                                    ExtractorLink(
                                        name,
                                        "$name ${extractedRes}p",
                                        extractedUrlM3u8,
                                        extractedUrl,
                                        getQualityFromName(extractedRes),
                                        true
                                    )
                                )*/
                            }
                    }
                        .toList()
                } else emptyList()
            }
                .toList()
                .flatten()
        }
            .orEmpty()
    }
}

object Mp4Upload : Extractor {
    override val name: String = "Mp4Upload"
    override val mainUrl: String = "https://www.mp4upload.com"
    private val srcRegex = Regex("""player\.src\("(.*?)"""")
    val requiresReferer = true

    override fun getUrl(url: String): List<Storage> {
        with(com.programmersbox.gsonutils.getApi(url)) {
            getAndUnpack(this.orEmpty()).let { unpackedText ->
                srcRegex.find(unpackedText)?.groupValues?.get(1)?.let { link ->
                    return listOf(
                        Storage(
                            link = link,
                            source = url,
                            filename = name,
                            quality = name,
                            sub = Qualities.Unknown.value.toString()
                        )
                    )
                }
            }
        }
        return emptyList()
    }
}

object MixDrop : Extractor {
    override val name: String = "MixDrop"
    override val mainUrl: String = "https://mixdrop.co"
    private val srcRegex = Regex("""wurl.*?=.*?"(.*?)";""")
    val requiresReferer = false

    fun getExtractorUrl(id: String): String = "$mainUrl/e/$id"

    fun httpsify(url: String): String = if (url.startsWith("//")) "https:$url" else url

    override fun getUrl(url: String): List<Storage> {
        with(Jsoup.connect(url.replace("$mainUrl/f", "$mainUrl/e")).get().html()) {
            getAndUnpack(this.orEmpty()).let { unpackedText ->
                srcRegex.find(unpackedText)?.groupValues?.get(1)?.let { link ->
                    return listOf(
                        Storage(
                            link = httpsify(link),
                            source = url,
                            filename = name,
                            quality = "$name - Stream Only",
                            sub = Qualities.Unknown.value.toString()
                        )
                    )
                }
            }
        }
        return emptyList()
    }
}

object DoodToExtractor : DoodLaExtractor() {
    override val mainUrl: String
        get() = "https://dood.to"
}

object DoodSoExtractor : DoodLaExtractor() {
    override val mainUrl: String
        get() = "https://dood.so"
}

open class DoodLaExtractor : Extractor {
    override val name: String
        get() = "DoodStream"
    override val mainUrl: String
        get() = "https://dood.la"
    val requiresReferer: Boolean get() = false

    fun getExtractorUrl(id: String): String {
        return "$mainUrl/d/$id"
    }

    override fun getUrl(url: String): List<Storage> {
        val id = url.removePrefix("$mainUrl/e/").removePrefix("$mainUrl/d/")
        val trueUrl = getExtractorUrl(id)
        val response = com.programmersbox.gsonutils.getApi(trueUrl).orEmpty()
        Regex("href=\".*/download/(.*?)\"").find(response)?.groupValues?.get(1)?.let { link ->
            if (link.isEmpty()) return emptyList()
            sleep(5000) // might need this to not trigger anti bot
            val downloadLink = "$mainUrl/download/$link"
            val downloadResponse = com.programmersbox.gsonutils.getApi(downloadLink).orEmpty()
            Regex("onclick=\"window\\.open\\((['\"])(.*?)(['\"])").find(downloadResponse)?.groupValues?.get(2)
                ?.let { trueLink ->
                    return listOf(
                        Storage(
                            link = trueLink,
                            source = mainUrl,
                            filename = name,
                            quality = " $name - Download Only",
                            sub = Qualities.Unknown.value.toString(),
                        )
                    )
                    // links are valid in 8h
                }
        }

        return emptyList()
    }
}