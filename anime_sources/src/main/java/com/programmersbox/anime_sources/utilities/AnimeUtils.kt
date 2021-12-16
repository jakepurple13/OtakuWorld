package com.programmersbox.anime_sources.utilities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.HashMap
import kotlin.math.pow

fun getApi(url: String, block: HttpUrl.Builder.() -> Unit = {}): ApiResponse = apiAccess(url, block) { get() }

fun postApi(url: String, block: HttpUrl.Builder.() -> Unit = {}): ApiResponse = apiAccess(url, block) {
    post(object : RequestBody() {
        override fun contentType(): MediaType? = "application/json".toMediaTypeOrNull()
        override fun writeTo(sink: BufferedSink) {}
    })
}

fun postApiMethod(
    url: String,
    block: HttpUrl.Builder.() -> Unit = {},
    method: Request.Builder.() -> Request.Builder
): ApiResponse = apiAccess(url, block) {
    post(object : RequestBody() {
        override fun contentType(): MediaType? = "application/json".toMediaTypeOrNull()
        override fun writeTo(sink: BufferedSink) {}
    })
        .method()
}

fun postApi(url: String, built: Request.Builder.() -> Request.Builder = { this }, block: HttpUrl.Builder.() -> Unit = {}): ApiResponse =
    apiAccess(url, block) {
        post(object : RequestBody() {
            override fun contentType(): MediaType? = "application/json".toMediaTypeOrNull()
            override fun writeTo(sink: BufferedSink) {}
        })
            .built()
    }

private fun apiAccess(url: String, block: HttpUrl.Builder.() -> Unit, method: Request.Builder.() -> Request.Builder): ApiResponse {
    val request = Request.Builder()
        .url(url.toHttpUrlOrNull()!!.newBuilder().apply(block).build())
        .cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
        .method()
        .build()
    val client = OkHttpClient().newCall(request).execute()
    return if (client.code == 200) ApiResponse.Success(client.body!!.string()) else ApiResponse.Failed(client.code)
}

sealed class ApiResponse {
    data class Success(val body: String) : ApiResponse()
    data class Failed(val code: Int) : ApiResponse()
}

inline fun <reified T> String?.debugJson(): T? = try {
    Gson().fromJson(this, object : TypeToken<T>() {}.type)
} catch (e: Exception) {
    println(this)
    e.printStackTrace()
    null
}

/**
 * @param  packedJS javascript P.A.C.K.E.R. coded.
 */
class JsUnpacker(private val packedJS: String?) {

    /**
     * Detects whether the javascript is P.A.C.K.E.R. coded.
     *
     * @return true if it's P.A.C.K.E.R. coded.
     */
    fun detect(): Boolean {
        val js = packedJS!!.replace(" ", "")
        val p = Pattern.compile("eval\\(function\\(p,a,c,k,e,[rd]")
        val m = p.matcher(js)
        return m.find()
    }

    /**
     * Unpack the javascript
     *
     * @return the javascript unpacked or null.
     */
    fun unpack(): String? {
        val js = packedJS
        try {
            var p = Pattern.compile("""\}\s*\('(.*)',\s*(.*?),\s*(\d+),\s*'(.*?)'\.split\('\|'\)""", Pattern.DOTALL)
            var m = p.matcher(js)
            if (m.find() && m.groupCount() == 4) {
                val payload = m.group(1).replace("\\'", "'")
                val radixStr = m.group(2)
                val countStr = m.group(3)
                val symtab = m.group(4).split("\\|".toRegex()).toTypedArray()
                var radix = 36
                var count = 0
                try {
                    radix = radixStr.toInt()
                } catch (e: Exception) {
                }
                try {
                    count = countStr.toInt()
                } catch (e: Exception) {
                }
                if (symtab.size != count) {
                    throw Exception("Unknown p.a.c.k.e.r. encoding")
                }
                val unbase = Unbase(radix)
                p = Pattern.compile("\\b\\w+\\b")
                m = p.matcher(payload)
                val decoded = StringBuilder(payload)
                var replaceOffset = 0
                while (m.find()) {
                    val word = m.group(0)
                    val x = unbase.unbase(word)
                    var value: String? = null
                    if (x < symtab.size) {
                        value = symtab[x]
                    }
                    if (value != null && value.isNotEmpty()) {
                        decoded.replace(m.start() + replaceOffset, m.end() + replaceOffset, value)
                        replaceOffset += value.length - word.length
                    }
                }
                return decoded.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private inner class Unbase(private val radix: Int) {
        private val ALPHABET_62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val ALPHABET_95 =
            " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
        private var alphabet: String? = null
        private var dictionary: HashMap<String, Int>? = null
        fun unbase(str: String): Int {
            var ret = 0
            if (alphabet == null) {
                ret = str.toInt(radix)
            } else {
                val tmp = StringBuilder(str).reverse().toString()
                for (i in tmp.indices) {
                    ret += (radix.toDouble().pow(i.toDouble()) * dictionary!![tmp.substring(i, i + 1)]!!).toInt()
                }
            }
            return ret
        }

        init {
            if (radix > 36) {
                when {
                    radix < 62 -> {
                        alphabet = ALPHABET_62.substring(0, radix)
                    }
                    radix in 63..94 -> {
                        alphabet = ALPHABET_95.substring(0, radix)
                    }
                    radix == 62 -> {
                        alphabet = ALPHABET_62
                    }
                    radix == 95 -> {
                        alphabet = ALPHABET_95
                    }
                }
                dictionary = HashMap(95)
                for (i in 0 until alphabet!!.length) {
                    dictionary!![alphabet!!.substring(i, i + 1)] = i
                }
            }
        }
    }
}

enum class Qualities(var value: Int) {
    Unknown(0),
    P360(-2), // 360p
    P480(-1), // 480p
    P720(1), // 720p
    P1080(2), // 1080p
    P1440(3), // 1440p
    P2160(4) // 4k or 2160p
}

fun getQualityFromName(qualityName: String): Qualities {
    return when (qualityName.replace("p", "").replace("P", "")) {
        "360" -> Qualities.P360
        "480" -> Qualities.P480
        "720" -> Qualities.P720
        "1080" -> Qualities.P1080
        "1440" -> Qualities.P1440
        "2160" -> Qualities.P2160
        "4k" -> Qualities.P2160
        "4K" -> Qualities.P2160
        else -> Qualities.Unknown
    }
}

class M3u8Helper {
    private val ENCRYPTION_DETECTION_REGEX = Regex("#EXT-X-KEY:METHOD=([^,]+),")
    private val ENCRYPTION_URL_IV_REGEX = Regex("#EXT-X-KEY:METHOD=([^,]+),URI=\"([^\"]+)\"(?:,IV=(.*))?")
    private val QUALITY_REGEX =
        Regex("""#EXT-X-STREAM-INF:(?:(?:.*?(?:RESOLUTION=\d+x(\d+)).*?\s+(.*))|(?:.*?\s+(.*)))""")
    private val TS_EXTENSION_REGEX = Regex("""(.*\.ts.*)""")

    fun absoluteExtensionDetermination(url: String): String? {
        val split = url.split("/")
        val gg: String = split[split.size - 1].split("?")[0]
        return if (gg.contains(".")) {
            gg.split(".").ifEmpty { null }?.last()
        } else null
    }

    private fun toBytes16Big(n: Int): ByteArray {
        return ByteArray(16) {
            val fixed = n / 256.0.pow((15 - it))
            (maxOf(0, fixed.toInt()) % 256).toByte()
        }
    }

    private val defaultIvGen = sequence {
        var initial = 1

        while (true) {
            yield(toBytes16Big(initial))
            ++initial
        }
    }.iterator()

    private fun getDecrypter(secretKey: ByteArray, data: ByteArray, iv: ByteArray = "".toByteArray()): ByteArray {
        val ivKey = if (iv.isEmpty()) defaultIvGen.next() else iv
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val skSpec = SecretKeySpec(secretKey, "AES")
        val ivSpec = IvParameterSpec(ivKey)
        c.init(Cipher.DECRYPT_MODE, skSpec, ivSpec)
        return c.doFinal(data)
    }

    private fun isEncrypted(m3u8Data: String): Boolean {
        val st = ENCRYPTION_DETECTION_REGEX.find(m3u8Data)
        return st != null && (st.value.isNotEmpty() || st.destructured.component1() != "NONE")
    }

    data class M3u8Stream(
        val streamUrl: String,
        val quality: Int? = null,
        val headers: Map<String, String> = mapOf()
    )

    private fun selectBest(qualities: List<M3u8Stream>): M3u8Stream? {
        val result = qualities.sortedBy {
            if (it.quality != null && it.quality <= 1080) it.quality else 0
        }.filter {
            listOf("m3u", "m3u8").contains(absoluteExtensionDetermination(it.streamUrl))
        }
        return result.getOrNull(0)
    }

    private fun getParentLink(uri: String): String {
        val split = uri.split("/").toMutableList()
        split.removeLast()
        return split.joinToString("/")
    }

    private fun isNotCompleteUrl(url: String): Boolean {
        return !url.contains("https://") && !url.contains("http://")
    }

    fun m3u8Generation(m3u8: M3u8Stream, returnThis: Boolean): List<M3u8Stream> {
        val generate = sequence {
            val m3u8Parent = getParentLink(m3u8.streamUrl)
            val response = get(m3u8.streamUrl, headers = m3u8.headers).text

            for (match in QUALITY_REGEX.findAll(response)) {
                var (quality, m3u8Link, m3u8Link2) = match.destructured
                if (m3u8Link.isEmpty()) m3u8Link = m3u8Link2
                if (absoluteExtensionDetermination(m3u8Link) == "m3u8") {
                    if (isNotCompleteUrl(m3u8Link)) {
                        m3u8Link = "$m3u8Parent/$m3u8Link"
                    }
                    if (quality.isEmpty()) {
                        println(m3u8.streamUrl)
                    }
                    yieldAll(
                        m3u8Generation(
                            M3u8Stream(
                                m3u8Link,
                                quality.toIntOrNull(),
                                m3u8.headers
                            ), false
                        )
                    )
                }
                yield(
                    M3u8Stream(
                        m3u8Link,
                        quality.toIntOrNull(),
                        m3u8.headers
                    )
                )
            }
            if (returnThis) {
                yield(
                    M3u8Stream(
                        m3u8.streamUrl,
                        0,
                        m3u8.headers
                    )
                )
            }
        }
        return generate.toList()
    }

    data class HlsDownloadData(
        val bytes: ByteArray,
        val currentIndex: Int,
        val totalTs: Int,
        val errored: Boolean = false
    )

    fun hlsYield(qualities: List<M3u8Stream>, startIndex: Int = 0): Iterator<HlsDownloadData> {
        if (qualities.isEmpty()) return listOf(HlsDownloadData(byteArrayOf(), 1, 1, true)).iterator()

        var selected = selectBest(qualities)
        if (selected == null) {
            selected = qualities[0]
        }
        val headers = selected.headers

        val streams = qualities.map { m3u8Generation(it, false) }.flatten()
        //val sslVerification = if (headers.containsKey("ssl_verification")) headers["ssl_verification"].toBoolean() else true

        val secondSelection = selectBest(streams.ifEmpty { listOf(selected) })
        if (secondSelection != null) {
            val m3u8Response = get(secondSelection.streamUrl, headers = headers).text

            var encryptionUri: String?
            var encryptionIv = byteArrayOf()
            var encryptionData = byteArrayOf()

            val encryptionState = isEncrypted(m3u8Response)

            if (encryptionState) {
                val match =
                    ENCRYPTION_URL_IV_REGEX.find(m3u8Response)!!.destructured  // its safe to assume that its not going to be null
                encryptionUri = match.component2()

                if (isNotCompleteUrl(encryptionUri)) {
                    encryptionUri = "${getParentLink(secondSelection.streamUrl)}/$encryptionUri"
                }

                encryptionIv = match.component3().toByteArray()
                val encryptionKeyResponse = get(encryptionUri, headers = headers)
                encryptionData = encryptionKeyResponse.body?.bytes() ?: byteArrayOf()
            }

            val allTs = TS_EXTENSION_REGEX.findAll(m3u8Response)
            val allTsList = allTs.toList()
            val totalTs = allTsList.size
            if (totalTs == 0) {
                return listOf(HlsDownloadData(byteArrayOf(), 1, 1, true)).iterator()
            }
            var lastYield = 0

            val relativeUrl = getParentLink(secondSelection.streamUrl)
            var retries = 0
            val tsByteGen = sequence {
                loop@ for ((index, ts) in allTs.withIndex()) {
                    val url = if (
                        isNotCompleteUrl(ts.destructured.component1())
                    ) "$relativeUrl/${ts.destructured.component1()}" else ts.destructured.component1()
                    val c = index + 1 + startIndex

                    while (lastYield != c) {
                        try {
                            val tsResponse = get(url, headers = headers)
                            var tsData = tsResponse.body?.bytes() ?: byteArrayOf()

                            if (encryptionState) {
                                tsData = getDecrypter(encryptionData, tsData, encryptionIv)
                                yield(HlsDownloadData(tsData, c, totalTs))
                                lastYield = c
                                break
                            }
                            yield(HlsDownloadData(tsData, c, totalTs))
                            lastYield = c
                        } catch (e: Exception) {
                            if (retries == 3) {
                                yield(HlsDownloadData(byteArrayOf(), c, totalTs, true))
                                break@loop
                            }
                            ++retries
                            Thread.sleep(2_000)
                        }
                    }
                }
            }
            return tsByteGen.iterator()
        }
        return listOf(HlsDownloadData(byteArrayOf(), 1, 1, true)).iterator()
    }
}

private const val DEFAULT_TIME = 10
private val DEFAULT_TIME_UNIT = TimeUnit.MINUTES
private const val DEFAULT_USER_AGENT = ""
private val DEFAULT_HEADERS = mapOf("User-Agent" to DEFAULT_USER_AGENT)
private val DEFAULT_DATA: Map<String, String> = mapOf()
private val DEFAULT_COOKIES: Map<String, String> = mapOf()
private val DEFAULT_REFERER: String? = null


/** WARNING! CAN ONLY BE READ ONCE */
val Response.text: String
    get() {
        return this.body?.string() ?: ""
    }

val Response.url: String
    get() {
        return this.request.url.toString()
    }

val Response.cookies: Map<String, String>
    get() {
        val cookieList =
            this.headers.filter { it.first.toLowerCase(Locale.ROOT) == "set-cookie" }.getOrNull(0)?.second?.split(";")
        return cookieList?.associate {
            val split = it.split("=")
            (split.getOrNull(0)?.trim() ?: "") to (split.getOrNull(1)?.trim() ?: "")
        }?.filter { it.key.isNotBlank() && it.value.isNotBlank() } ?: mapOf()
    }

fun getData(data: Map<String, String>): RequestBody {
    val builder = FormBody.Builder()
    data.forEach {
        builder.add(it.key, it.value)
    }
    return builder.build()
}

// https://github.com, id=test -> https://github.com?id=test
fun appendUri(uri: String, appendQuery: String): String {
    val oldUri = URI(uri)
    return URI(
        oldUri.scheme, oldUri.authority, oldUri.path,
        if (oldUri.query == null) appendQuery else oldUri.query + "&" + appendQuery, oldUri.fragment
    ).toString()
}

// Can probably be done recursively
fun addParamsToUrl(url: String, params: Map<String, String>): String {
    var appendedUrl = url
    params.forEach {
        appendedUrl = appendUri(appendedUrl, "${it.key}=${it.value}")
    }
    return appendedUrl
}

fun getCache(cacheTime: Int, cacheUnit: TimeUnit): CacheControl {
    return CacheControl.Builder().maxAge(cacheTime, cacheUnit).build()
}

/**
 * Referer > Set headers > Set cookies > Default headers > Default Cookies
 */
fun getHeaders(headers: Map<String, String>, referer: String?, cookie: Map<String, String>): Headers {
    val refererMap = (referer ?: DEFAULT_REFERER)?.let { mapOf("referer" to it) } ?: mapOf()
    val cookieHeaders = (DEFAULT_COOKIES + cookie)
    val cookieMap =
        if (cookieHeaders.isNotEmpty()) mapOf("Cookie" to cookieHeaders.entries.joinToString(separator = "; ") {
            "${it.key}=${it.value};"
        }) else mapOf()
    val tempHeaders = (DEFAULT_HEADERS + cookieMap + headers + refererMap)
    return tempHeaders.toHeaders()
}

fun get(
    url: String,
    headers: Map<String, String> = mapOf(),
    referer: String? = null,
    params: Map<String, String> = mapOf(),
    cookies: Map<String, String> = mapOf(),
    allowRedirects: Boolean = true,
    cacheTime: Int = DEFAULT_TIME,
    cacheUnit: TimeUnit = DEFAULT_TIME_UNIT,
    timeout: Long = 0L,
    interceptor: Interceptor? = null
): Response {

    val client = OkHttpClient().newBuilder()
        .followRedirects(allowRedirects)
        .followSslRedirects(allowRedirects)
        .callTimeout(timeout, TimeUnit.SECONDS)

    if (interceptor != null) client.addInterceptor(interceptor)

    val request = getRequestCreator(url, headers, referer, params, cookies, cacheTime, cacheUnit)
    return client.build().newCall(request).execute()
}


fun post(
    url: String,
    headers: Map<String, String> = mapOf(),
    referer: String? = null,
    params: Map<String, String> = mapOf(),
    cookies: Map<String, String> = mapOf(),
    data: Map<String, String> = DEFAULT_DATA,
    allowRedirects: Boolean = true,
    cacheTime: Int = DEFAULT_TIME,
    cacheUnit: TimeUnit = DEFAULT_TIME_UNIT,
    timeout: Long = 0L
): Response {
    val client = OkHttpClient().newBuilder()
        .followRedirects(allowRedirects)
        .followSslRedirects(allowRedirects)
        .callTimeout(timeout, TimeUnit.SECONDS)
        .build()
    val request = postRequestCreator(url, headers, referer, params, cookies, data, cacheTime, cacheUnit)
    return client.newCall(request).execute()
}


fun getRequestCreator(
    url: String,
    headers: Map<String, String>,
    referer: String?,
    params: Map<String, String>,
    cookies: Map<String, String>,
    cacheTime: Int,
    cacheUnit: TimeUnit
): Request {
    return Request.Builder()
        .url(addParamsToUrl(url, params))
        .cacheControl(getCache(cacheTime, cacheUnit))
        .headers(getHeaders(headers, referer, cookies))
        .build()
}

fun postRequestCreator(
    url: String,
    headers: Map<String, String>,
    referer: String?,
    params: Map<String, String>,
    cookies: Map<String, String>,
    data: Map<String, String>,
    cacheTime: Int,
    cacheUnit: TimeUnit
): Request {
    return Request.Builder()
        .url(addParamsToUrl(url, params))
        .cacheControl(getCache(cacheTime, cacheUnit))
        .headers(getHeaders(headers, referer, cookies))
        .post(getData(data))
        .build()
}

fun fixUrl(url: String, baseUrl: String): String {
    if (url.startsWith("http")) {
        return url
    }

    val startsWithNoHttp = url.startsWith("//")
    if (startsWithNoHttp) {
        return "https:$url"
    } else {
        if (url.startsWith('/')) {
            return baseUrl + url
        }
        return "${baseUrl}/$url"
    }
}