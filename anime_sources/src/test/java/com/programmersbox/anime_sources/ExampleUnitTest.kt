package com.programmersbox.anime_sources

import com.programmersbox.anime_sources.anime.AnimeToonDubbed
import com.programmersbox.anime_sources.anime.YTSQuery
import com.programmersbox.anime_sources.anime.YtsService
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() = runBlocking {
        val f = YtsService.build()//getJsonApi<Base>("https://yts.mx/api/v2/list_movies.json")
        val f1 = f.listMovies(YTSQuery.ListMoviesBuilder.getDefault().setQuery("big bang").build())
        val movies = f1?.blockingGet()?.data?.movies
        println(movies?.joinToString("\n"))

        val m1 = f.getMovie(
            YTSQuery.MovieBuilder().setMovieId(movies?.first()?.id?.toInt() ?: 30478).build()
        )

        println(m1.blockingGet())

    }

    @Test
    fun animetoon() {
        val f = AnimeToonDubbed

        val r = f.getRecent().blockingGet()

        println(r)

        val d = r.first().toInfoModel().blockingGet()

        println(d)

        val v = d.chapters.first().getChapterInfo().doOnError { }.blockingGet()

        println(v)
    }

}