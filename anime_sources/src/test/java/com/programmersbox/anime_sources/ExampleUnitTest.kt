package com.programmersbox.anime_sources

import androidx.annotation.WorkerThread
import androidx.compose.ui.util.fastMap
import com.programmersbox.anime_sources.anime.*
import com.programmersbox.gsonutils.getApi
import com.programmersbox.gsonutils.header
import com.programmersbox.models.ItemModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.BufferedSink
import org.jsoup.Jsoup
import org.junit.Test
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun allanimeTest() = runBlocking {
        val f = AllAnime.recent(1)
        println(f)
        val f1 = f.first().toInfoModel().first().getOrNull()
        println(f1)
        val f2 = f1?.chapters?.first()?.getChapterInfo()?.first()
        println(f2)
    }

    @Test
    fun kawaiifuTest() {
        val url = "https://kawaiifu.com"
        val f = Jsoup.connect(url)
            .sslSocketFactory(socketFactory())
            .get()

        val recent = f.select(".today-update .item").fastMap {
            ItemModel(
                title = it.selectFirst("img")?.attr("alt").orEmpty(),
                description = it.select("div.info").select("p").text(),
                imageUrl = it.selectFirst("img")?.attr("src").orEmpty(),
                url = it.selectFirst("a")?.attr("href").orEmpty(),
                source = Kawaiifu
            )
        }

        println(recent.joinToString("\n"))

    }

    @Test
    fun allmoviesforyouTest() {
        val url = "https://allmoviesforyou.co"
        val f = Jsoup.connect(url)
            .sslSocketFactory(socketFactory())
            .get()
        println(f)
    }

    private fun socketFactory(): SSLSocketFactory? {
        val trustAllCerts: Array<TrustManager> = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOfNulls(0)

            override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
            override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        })
        return try {
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            sslContext.socketFactory
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to create a SSL socket factory", e)
        } catch (e: KeyManagementException) {
            throw RuntimeException("Failed to create a SSL socket factory", e)
        }
    }

    @Test
    fun gogoanimevcTest() = runBlocking {

        println(listOf(-1, -2, 1, 2, 3, 0).sortedByDescending { it })

        val f = GogoAnimeVC.recent()
        //println(f.joinToString("\n"))
        val e = f.random().toInfoModel().first().getOrThrow()
        println(e)
        val c = e.chapters.first().getChapterInfo().first()
        println(c.joinToString("\n"))

        //val a = GogoAnimeVC.searchList("Mushi", 1, emptyList()).blockingGet()
        //println(a.joinToString("\n"))
    }

    @Test
    fun putlockerTest() = runBlocking {

        //val f = "http://putlockers.fm/recently-added.html".toJsoup()
        //println(f)

        //PutlockerTV.IS_TEST = true

        /*val list = PutlockerTV.getList().blockingGet().randomOrNull()
            ?.toInfoModel()?.blockingGet()
            ?.chapters?.firstOrNull()
            ?.getChapterInfo()?.blockingGet()?.firstOrNull()
        //println(list.joinToString("\n"))
        println(list?.link)*/

        val search = PutlockerTV.searchListFlow("the mask", list = emptyList()).first()
        println(search.first().toInfoModel().first())

        //println(list?.link?.toJsoup())

        /*val items = f.select("ul.list").select("div.item").map { it.select("a.thumb").attr("abs:href") }
        //println(items.joinToString("\n"))

        val d = items.firstOrNull()?.toJsoup()
        //println(d)

        val regex = "Base64.decode\\(\"(.*)\"\\)".toRegex().find(d?.toString().orEmpty())?.groups?.get(1)?.value

        //println(regex)

        val b = Jsoup.parse(String(Base64.getDecoder().decode(regex))).select("iframe").attr("abs:src")
        //println(b)

        val links = b.toJsoup()
        println(links)*/

    }

    @Test
    fun watchmovieTest() {
        val f = Jsoup.connect("https://watchmovie.movie/search.html").data("keyword", "One").get()
        println(f)
    }

    @Test
    fun animesimpleTest() = runBlocking {

        //val f = AnimeSimpleSubbed.getList().blockingGet()

        //println(f.joinToString("\n"))

        //println("https://ww1.animesimple.com/watch/197438-shokugeki-no-souma-shin-no-sara-episode-12-anime.html".toJsoup())

        //println("https://ww1.animesimple.com".toJsoup())

        val f = AnimeSimpleSubbed.searchListFlow("mushi", list = emptyList()).first()
        //println(f.joinToString("\n"))
        val d = f.random().toInfoModel().first().getOrThrow()
        println(d)
        val e = d.chapters.first().getChapterInfo().first().first().link
        println(e)
        println(e?.toJsoup())

        /*val f = com.programmersbox.anime_sources.utilities.getApi("https://animesimple.com/search") {
            addEncodedQueryParameter("q", "mushishi")
        }

        //println(f)

        //ww1.animesimple.com/series-list/

        val s = Jsoup.parse((f as ApiResponse.Success).body)
            .select("div#explore-container")
            .select("div.card-query")
            .map {
                ItemModel(
                    title = it.select("div.card[title]").attr("title"),
                    description = "",
                    imageUrl = it.select("img.card-img-top").attr("abs:src"),
                    url = "https:${it.select("h6.card-text a[href]").attr("href")}",
                    source = Sources.VIDSTREAMING
                )
            }

        println(s)

        val model = s.first()

        val doc = model.url.toJsoup()

        //println(doc)

        val id2 = com.programmersbox.anime_sources.utilities.getApi("https://animesimple.com/request") {
            addEncodedQueryParameter("anime-id", doc.select("input#animeid").attr("value"))
            addEncodedQueryParameter("epi-page", "1")
            addEncodedQueryParameter("top", "10000")
            addEncodedQueryParameter("bottom", "0")
        }.let { (it as? ApiResponse.Success)?.body }

        val id = InfoModel(
            title = model.title,
            description = doc.select("p.synopsis").text(),
            url = model.url,
            imageUrl = doc.select("img#series-image").attr("abs:src"),
            chapters = Jsoup.parse(id2).select("a.list-group-item").map {
                ChapterModel(
                    name = it.text(),
                    url = "https:${it.select("a").attr("href")}",
                    uploaded = "",
                    sourceUrl = model.url,
                    source = Sources.VIDSTREAMING
                )
            },
            genres = doc.select("a.badge, a.badge-secondary").select("a").eachText(),
            alternativeNames = emptyList(),
            source = Sources.VIDSTREAMING
        )

        println(id)

        val e = id.chapters.first().url.toJsoup()

        //println(e)

        val r = "var json = ([^;]*)".toRegex().find(e.toString())?.groups?.get(1)?.value?.fromJson<List<AnimeSimpleEpisode>>()

        println(r)

        val streamUrls = r?.map {

            val u = "src=['|\\\"]([^\\'|^\\\"]*)".toRegex().find(it.player!!)?.groups?.get(1)?.value

            Storage(
                link = u,
                source = it.host,
                quality = "Good",
                sub = it.type.toString()
            )
        }

        println(streamUrls)

        val url = streamUrls?.first()?.link!!

        val u = Jsoup.connect(url).header("Referer", "https://anistream.xyz").get()

        //println(u)

        val source = "<source src=\"(.*?)\"".toRegex()
        val token = "token\\s*=\\s*['\\\"|']([^\\\"']*)".toRegex()

        val finalUrl = when {
            source.containsMatchIn(u.toString()) -> source.find(u.toString())?.groups?.get(1)?.value
            token.containsMatchIn(u.toString()) -> {
                val id3 = url.split("/").last()
                Jsoup.connect("https://mp4.sh/v/$id3")
                    .header("Referer", url)
                    .data("token", token.find(u.toString())?.groups?.get(1)?.value)
                    .ignoreContentType(true)
                    .post()
                    .text()
                    .fromJson<AnimeSimpleFinalUrl>()
                    ?.file
            }
            else -> ""
        }

        println(finalUrl)*/

    }

    @Test
    fun animeflixTest() {

        val res = com.programmersbox.anime_sources.utilities.getApi("https://animeflix.io/api/search") {
            addEncodedQueryParameter("q", "Mushi")
        }

        println(res)

        /*val streamUrl = "https://animeflix.io/api/videos?episode_id"
        val episodeApi = "https://animeflix.io/api/episode"

        val a = "var episode = (.*?)\\}".toRegex().find(e.toString())?.groups?.get(1)?.value?.let { "$it}" }?.fromJson<AnimeFlixEpisode>()

        println(a)

        *//*val res = com.programmersbox.anime_sources.utilities.getApi(episodeApi) {
            addEncodedQueryParameter("episode_num", a?.episode?.toString())
            addEncodedQueryParameter("slug", a?.?.toString())
        }*/
    }

    data class AnimeFlixEpisode(
        val name: String?,
        val episode: Number?,
        val anime_id: Number?,
        val mal_id: Number?,
        val episode_id: Number?,
        val next_episode_url: String?
    )

    @Test
    fun vidStreamingTest() = runBlocking {

        //val f = "https://vidstreaming.io/popular".toJsoup()
        //println(f)

        //val f = Vidstreaming.getList().blockingGet().firstOrNull()?.toInfoModel()?.blockingGet()

        //println(f)

        val f = Vidstreaming.searchListFlow("cheat kusushi", list = emptyList()).first().firstOrNull()
        println(f)

        val e = f?.toInfoModel()?.first()?.getOrThrow()?.chapters?.firstOrNull()?.getChapterInfo()?.first()
        println(e)

        /*val e = "https://vidstreaming.io/videos/tensei-shitara-slime-datta-ken-episode-24-9".toJsoup()
        //println(e)

        val v = e.select("div.play-video").select("iframe").attr("abs:src")
        println(v)

        val s = v.toJsoup()
        //println(s)
        val links = s.select("li.linkserver")
        println(links.joinToString("\n"))

        val xstream = links.find { it.text() == "Xstreamcdn" }?.attr("abs:data-video")
        println(xstream)

        //val x = xstream?.toJsoup()
        //println(x)

        val xApi = "https://fcdn.stream/api/source/${xstream?.split("/")?.last()}"
        println(xApi)
        val api = getApiPost(xApi).fromJson<Xstream>()
        println(api)
        val file = api?.data?.firstOrNull()?.file
        println(getApi(file!!))*/
    }

    @Test
    fun fboxTest() {
        //val f = "https://putlockernew.site/all-movies".toJsoup()//"https://putlockernew.site".toJsoup()
        //println(f)

        val g = "https://putlockernew.site/watch-movie/america-the-motion-picture-2021_cw9q88erb/976x909-full-movie-online?watchnow=1".toJsoup()
        //println(g)

        val d = g.select("div.show_player").select("iframe").attr("abs:src").toJsoup()
        //println(d)

        //val mix = "<iframe[^>]+src=\"([^\"]+)\"[^>]*><\\/iframe>".toRegex().find(doc.toString())!!.groups[1]!!.value
        val doc2 = d//Jsoup.connect(mix.trim()).get()
        val r = "\\}\\('(.+)',(\\d+),(\\d+),'([^']+)'\\.split\\('\\|'\\)".toRegex().find(doc2.toString())!!
        fun encodeBaseN(num: Int, n: Int): String {
            var num1 = num
            val fullTable = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            //println("$num - $n")
            val table = fullTable.substring(0 until n)
            if (num1 == 0) return table[0].toString()
            var ret = ""
            while (num1 > 0) {
                ret = (table[num1 % n].toString() + ret)
                num1 = Math.floorDiv(num, n)
                if (num1 == 1) num1 = 0
                //println("$ret - $num1")
            }
            return ret
        }
        val (obfucastedCode, baseTemp, countTemp, symbolsTemp) = r.destructured
        //println(r.destructured.toList().joinToString("\n"))
        val base = baseTemp.toInt()
        var count = countTemp.toInt()
        val symbols = symbolsTemp.split("|")
        val symbolTable = mutableMapOf<String, String>()
        while (count > 0) {
            count--
            val baseNCount = encodeBaseN(count, base)
            symbolTable[baseNCount] = if (symbols[count].isNotEmpty()) symbols[count] else baseNCount
        }
        val unpacked = "\\b(\\w+)\\b".toRegex().replace(obfucastedCode) { symbolTable[it.groups[0]!!.value].toString() }
        println(unpacked)
        val search = "MDCore\\.v.*?=\"([^\"]+)".toRegex().find(unpacked)?.groups?.get(1)?.value
        //"https:$search"
        println(search)
    }

    @Test
    fun kickassanimeTest() {

        val api = com.programmersbox.anime_sources.utilities.getApi("https://kickassanime.rs/search") {
            addEncodedQueryParameter("q", "mushi")
        }

        println(api)

    }

    @Test
    fun animekisaTest() = runBlocking {

        /*val f = AnimeKisaSubbed.getList().blockingGet().take(10)
        println(f.joinToString("\n"))

        val e = f.first().toInfoModel().blockingGet()
        println(e)

        val c = e.chapters.first().getChapterInfo().blockingGet()
        println(c)*/

        val f = AnimeKisaSubbed.searchListFlow("mushi", list = emptyList()).first().take(10)
        println(f.joinToString("\n"))

        //val v = c.first().link?.toJsoup()
        //println(v)

    }

    @WorkerThread
    fun getApiPost(url: String, builder: okhttp3.Request.Builder.() -> Unit = {}): String? {
        val request = okhttp3.Request.Builder()
            .url(url)
            .apply(builder)
            .post(object : RequestBody() {
                override fun contentType(): MediaType? = "application/json".toMediaTypeOrNull()

                override fun writeTo(sink: BufferedSink) {
                }

            })
            .build()
        val response = OkHttpClient().newCall(request).execute()
        return if (response.code == 200) response.body!!.string() else null
    }

    @Test
    fun wcostreamTest() = runBlocking {

        /*val f = WcoDubbed.getList().blockingGet()

        println(f.size)

        val i = f.find { it.title == "Mushishi" }!!.toInfoModel().blockingGet()

        println(i)

        val e = i.chapters.first().getChapterInfo().blockingGet()

        println(e)*/

        /*val f = Jsoup.connect("https://www.wcostream.com/wonder-egg-priority-episode-7-english-dubbed").get()
            .select("div.ildate").select("a").attr("abs:href")
        println(f)*/

        //val f1 = WcoDubbed.getRecent().blockingGet()

        //println(f1.size)
        //println(f1.joinToString("\n"))

        //println(WcoDubbed.getSourceByUrl("https://www.wcostream.com/anime/mushi-shi-english-dubbed-guide"))

        /*val f = WcoDubbed.searchList("one", list = emptyList()).blockingGet()

        println(f.joinToString("\n"))*/

    }

    @Test
    fun animetoontvTest() {

        val f = "http://api.animetoon.tv/GetNewDubbed"

        val doc = getApi(f) {
            header(
                "User-Agent" to "okhttp/2.3.0",
                "App-LandingPage" to "http://www.mobi24.net/toon.html",
                "App-Name" to "#Toonmania",
                "Connection" to "Keep-Alive",
                "Host" to "api.animetoon.tv",
                "App-Version" to "7.7"
            )
        }

        println(doc)

    }

    @Test
    fun animetoon() = runBlocking {
        val f = AnimeToonDubbed

        val r = f.recent()

        println(r)

        val d = r.first().toInfoModel().first().getOrThrow()

        println(d)

        val v = d.chapters.first().getChapterInfo().first()

        println(v)

        println(Jsoup.connect(v.random().link!!).get())
    }

}

