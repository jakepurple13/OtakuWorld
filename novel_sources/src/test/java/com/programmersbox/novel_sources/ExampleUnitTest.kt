package com.programmersbox.novel_sources

import com.programmersbox.gsonutils.getJsonApi
import com.programmersbox.novel_sources.novels.WuxiaWorld
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun novelTest2() = runBlocking {

        //val f = WuxiaWorld.getList(2).blockingGet()

        //println(f)

        //https://wuxiaworld.online/236130/start-a-dungeon

        println(WuxiaWorld.getSourceByUrl("https://wuxiaworld.online/236130/start-a-dungeon"))

    }

    @Test
    fun novelTest() {

        val baseUrl = "https://api.skynovels.net/api"

        //https://api.skynovels.net/api/home-updated-novel-chapters/1
        //https://api.skynovels.net/api/home
        //https://api.skynovels.net/api/get-image/[image-blob]
        //https://www.skynovels.net/novelas/17/i-shall-seal-the-heavens
        //https://www.skynovels.net/novelas/[author id]/[nvl name]
        //https://api.skynovels.net/api/get-image/RGckVBnDR5Rd1xqG5kr8C36F.png/novels/false
        val url = "http://www.skynovels.net"

        //println(url.toJsoup())

        val b = getJsonApi<SkyNovels.Base>("$baseUrl/home")?.recentNovels?.first()
        println(b)

        val c = "$url/novelas/${b?.id}/${b?.nvl_name}".toJsoup()
        println(c)

    }

    object SkyNovels {

        data class Base(
            val topNovels: List<TopNovels>?,
            val recentNovels: List<RecentNovels>?,
            val recommendedNovel: List<RecommendedNovel>?,
            val updatedNovels: List<UpdatedNovels>?,
            val finishedNovels: List<FinishedNovels>?
        )

        data class FinishedNovels(
            val id: Number?,
            val nvl_author: Number?,
            val nvl_content: String?,
            val nvl_title: String?,
            val nvl_acronym: String?,
            val nvl_status: String?,
            val nvl_publication_date: String?,
            val nvl_name: String?,
            val nvl_recommended: Number?,
            val nvl_writer: String?,
            val nvl_translator: String?,
            val nvl_translator_eng: String?,
            val image: String?,
            val createdAt: String?,
            val updatedAt: String?,
            val nvl_rating: Number?
        )

        data class Genres(val id: Number?, val genre_name: String?)

        data class RecentNovels(
            val id: Number?,
            val nvl_author: Number?,
            val nvl_content: String?,
            val nvl_title: String?,
            val nvl_acronym: String?,
            val nvl_status: String?,
            val nvl_publication_date: Any?,
            val nvl_name: String?,
            val nvl_recommended: Number?,
            val nvl_writer: String?,
            val nvl_translator: String?,
            val nvl_translator_eng: String?,
            val image: String?,
            val createdAt: String?,
            val updatedAt: String?,
            val nvl_rating: Number?
        )

        data class RecommendedNovel(
            val id: Number?,
            val nvl_author: Number?,
            val nvl_content: String?,
            val nvl_title: String?,
            val nvl_acronym: String?,
            val nvl_status: String?,
            val nvl_publication_date: String?,
            val nvl_name: String?,
            val nvl_recommended: Number?,
            val nvl_writer: String?,
            val nvl_translator: Any?,
            val nvl_translator_eng: Any?,
            val image: String?,
            val createdAt: String?,
            val updatedAt: String?,
            val nvl_chapters: Number?,
            val nvl_last_update: String?,
            val nvl_rating: Number?,
            val genres: List<Genres>?
        )

        data class TopNovels(
            val id: Number?,
            val nvl_author: Number?,
            val nvl_content: String?,
            val nvl_title: String?,
            val nvl_acronym: String?,
            val nvl_status: String?,
            val nvl_publication_date: String?,
            val nvl_name: String?,
            val nvl_recommended: Number?,
            val nvl_writer: String?,
            val nvl_translator: String?,
            val nvl_translator_eng: String?,
            val image: String?,
            val createdAt: String?,
            val updatedAt: String?,
            val nvl_rating: Number?,
            val nvl_ratings_count: Number?
        )

        data class UpdatedNovels(
            val id: Number?,
            val nvl_author: Number?,
            val nvl_content: String?,
            val nvl_title: String?,
            val nvl_acronym: String?,
            val nvl_status: String?,
            val nvl_publication_date: String?,
            val nvl_name: String?,
            val nvl_recommended: Number?,
            val nvl_writer: String?,
            val nvl_translator: String?,
            val nvl_translator_eng: String?,
            val image: String?,
            val createdAt: String?,
            val updatedAt: String?,
            val nvl_last_update: String?
        )
    }
}