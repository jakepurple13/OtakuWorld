package com.programmersbox.anime_sources.utilities

import androidx.compose.ui.util.fastMap
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.header
import com.programmersbox.models.Storage
import org.jsoup.Jsoup
import java.lang.Thread.sleep
import java.net.URI

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
    fun getExtractorUrl(id: String): String = ""
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

    override fun getExtractorUrl(id: String): String = "$mainUrl/api/source/$id"

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

    override fun getExtractorUrl(id: String): String = "$mainUrl/e/$id"

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

    override fun getExtractorUrl(id: String): String {
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

object WcoStreamExtractor : Extractor {
    override val name: String = "WcoStream"
    override val mainUrl: String = "https://vidstream.pro"
    val requiresReferer = false
    private val hlsHelper = M3u8Helper()

    data class WcoResponse(val success: Boolean?, val media: Media?)
    data class Media(val sources: List<WcoSources>?)
    data class WcoSources(val file: String)

    override fun getUrl(url: String): List<Storage> {
        val baseUrl = url.split("/e/")[0]

        val html = get(url, headers = mapOf("Referer" to "https://wcostream.cc/")).text
        val (Id) = "/e/(.*?)?domain".toRegex().find(url)!!.destructured
        val (skey) = """skey\s=\s['"](.*?)['"];""".toRegex().find(html.orEmpty())!!.destructured

        val apiLink = "$baseUrl/info/$Id?domain=wcostream.cc&skey=$skey"
        val referrer = "$baseUrl/e/$Id?domain=wcostream.cc"

        val response = get(apiLink, headers = mapOf("Referer" to referrer)).text.fromJson<WcoResponse>()

        return response?.media?.sources?.fastMap {
            if (it.file.contains("m3u8")) {
                hlsHelper.m3u8Generation(M3u8Helper.M3u8Stream(it.file, null), true)
                    .fastMap { stream ->
                        val qualityString = if ((stream.quality ?: 0) == 0) "" else "${stream.quality}p"
                        Storage(
                            link = stream.streamUrl,
                            source = mainUrl,
                            filename = name,
                            quality = "$name $qualityString",
                            sub = getQualityFromName(stream.quality.toString()).name,
                        )
                    }
            } else {
                listOf(
                    Storage(
                        link = it.file,
                        source = mainUrl,
                        filename = name,
                        quality = "720",
                        sub = Qualities.Unknown.value.toString(),
                    )
                )
            }
        }
            .orEmpty()
            .flatten()
    }
}

object MultiQuality : Extractor {
    override val name: String = "MultiQuality"
    override val mainUrl: String = "https://gogo-play.net"
    private val sourceRegex = Regex("""file:\s*['"](.*?)['"],label:\s*['"](.*?)['"]""")
    private val m3u8Regex = Regex(""".*?(\d*).m3u8""")
    private val urlRegex = Regex("""(.*?)([^/]+$)""")
    val requiresReferer = false

    override fun getExtractorUrl(id: String): String {
        return "$mainUrl/loadserver.php?id=$id"
    }

    override fun getUrl(url: String): List<Storage> {
        val extractedLinksList: MutableList<Storage> = mutableListOf()
        with(get(url)) {
            sourceRegex.findAll(this.text).forEach { sourceMatch ->
                val extractedUrl = sourceMatch.groupValues[1]
                // Trusting this isn't mp4, may fuck up stuff
                if (URI(extractedUrl).path.endsWith(".m3u8")) {
                    with(get(extractedUrl)) {
                        m3u8Regex.findAll(this.text).forEach { match ->
                            extractedLinksList.add(
                                Storage(
                                    link = urlRegex.find(this.url)!!.groupValues[1] + match.groupValues[0],
                                    source = url,
                                    filename = name,
                                    quality = getQualityFromName(match.groupValues[1]).name,
                                    sub = getQualityFromName(match.groupValues[1]).value.toString(),
                                )
                            )
                            /*extractedLinksList.add(
                                ExtractorLink(
                                    name,
                                    "$name ${match.groupValues[1]}p",
                                    urlRegex.find(this.url)!!.groupValues[1] + match.groupValues[0],
                                    url,
                                    getQualityFromName(match.groupValues[1]),
                                    isM3u8 = true
                                )
                            )*/
                        }

                    }
                } else if (extractedUrl.endsWith(".mp4")) {
                    extractedLinksList.add(
                        Storage(
                            link = extractedUrl,
                            source = url,
                            filename = name,
                            quality = "720",
                            sub = Qualities.Unknown.value.toString(),
                        )
                    )
                    /*extractedLinksList.add(
                        ExtractorLink(
                            name,
                            "$name ${sourceMatch.groupValues[2]}",
                            extractedUrl,
                            url.replace(" ", "%20"),
                            Qualities.Unknown.value,
                        )
                    )*/
                }
            }
            return extractedLinksList
        }
    }
}

class Vidstream(val mainUrl: String) {
    val name: String = "Vidstream"

    private fun getExtractorUrl(id: String): String {
        return "$mainUrl/streaming.php?id=$id"
    }

    private fun getDownloadUrl(id: String): String {
        return "$mainUrl/download?id=$id"
    }

    private val normalApis: List<Extractor> = listOf(MultiQuality)

    // https://gogo-stream.com/streaming.php?id=MTE3NDg5
    fun getUrl(id: String, sourceUrl: String): List<Storage> {
        try {
            val links = mutableListOf<Storage>()
            links.addAll(
                normalApis.flatMap { api ->
                    val url = api.getExtractorUrl(id)
                    api.getUrl(url)
                }
            )
            val extractorUrl = getExtractorUrl(id)

            val link = getDownloadUrl(id)
            println("Generated vidstream download link: $link")
            val page = get(link, referer = extractorUrl)

            val pageDoc = Jsoup.parse(page.text)
            val qualityRegex = Regex("(\\d+)P")

            links.addAll(
                pageDoc.select(".dowload > a[download]").map {
                    val qual = if (it.text().contains("HDP")) "1080"
                    else qualityRegex.find(it.text())?.destructured?.component1().toString()

                    Storage(
                        link = page.url,
                        source = sourceUrl,
                        filename = id,
                        quality = qual,
                        sub = Qualities.Unknown.value.toString(),
                    )

                    /*callback.invoke(
                        ExtractorLink(
                            this.name,
                            if (qual == "null") this.name else "${this.name} - " + qual + "p",
                            it.attr("href"),
                            page.url,
                            getQualityFromName(qual),
                            it.attr("href").contains(".m3u8")
                        )
                    )*/
                }
            )

            /*with(get(extractorUrl)) {
                val document = Jsoup.parse(this.text)
                val primaryLinks = document.select("ul.list-server-items > li.linkserver")
                println(primaryLinks)
                //val extractedLinksList: MutableList<ExtractorLink> = mutableListOf()

                // All vidstream links passed to extractors
                *//*links.addAll(
                    primaryLinks.distinctBy { it.attr("data-video") }.map { element ->
                        val link = element.attr("data-video")
                        //val name = element.text()

                        // Matches vidstream links with extractors
                        *//**//*Storage(
                            link = page.url,
                            source = sourceUrl,
                            filename = id,
                            quality = qual,
                            sub = Qualities.Unknown.value.toString(),
                        )*//**//*
                        *//**//*extractorApis.filter { !it.requiresReferer || !isCasting }.pmap { api ->
                            if (link.startsWith(api.mainUrl)) {
                                val extractedLinks = api.getSafeUrl(link, extractorUrl)
                                if (extractedLinks?.isNotEmpty() == true) {
                                    extractedLinks.forEach {
                                        callback.invoke(it)
                                    }
                                }
                            }
                        }*//**//*
                    }
                )*//*
                return links
            }*/
            return links
        } catch (e: Exception) {
            return emptyList()
        }
    }
}

object M3u8Manifest {
    // URL = first, QUALITY = second
    fun extractLinks(m3u8Data: String): ArrayList<Pair<String, String>> {
        val data: ArrayList<Pair<String, String>> = ArrayList()
        "\"(.*?)\":\"(.*?)\"".toRegex().findAll(m3u8Data).forEach {
            var quality = it.groupValues[1].replace("auto", "Auto")
            if (quality != "Auto" && !quality.endsWith('p')) quality += "p"
            val url = it.groupValues[2]
            data.add(Pair(url, quality))
        }
        return data
    }
}