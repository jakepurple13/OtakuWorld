package com.programmersbox.anime_sources.anime

import androidx.core.text.isDigitsOnly
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.models.*
import com.programmersbox.thirdpartyutils.gsonConverter
import com.programmersbox.thirdpartyutils.rx2FactoryAsync
import io.reactivex.Single
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.io.IOException
import kotlin.coroutines.resumeWithException

object Yts : ApiService {

    private val service by lazy { YtsService.build() }

    override val baseUrl: String get() = "https://yts.mx"
    override val serviceName: String get() = "YTS"
    override val canScroll: Boolean get() = true

    private val movieToModel: (Movies) -> ItemModel = {
        ItemModel(
            title = it.title.orEmpty(),
            description = it.description_full.orEmpty(),
            imageUrl = it.medium_cover_image.orEmpty(),
            url = it.url.orEmpty(),
            source = this
        ).apply { extras["info"] = it.toJson() }
    }

    private suspend fun Call.await(): Response {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isCancelled) return
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) {}
                }
            })
            continuation.invokeOnCancellation {
                try {
                    cancel()
                } catch (ex: Throwable) {
                }
            }
        }
    }

    override suspend fun getSourceByUrl(url: String): ItemModel? {
        try {
            val subResponse = OkHttpClient().newCall((Request.Builder().url(url).build())).await()
            if (!subResponse.isSuccessful) return null
            val subDoc = Jsoup.parse(subResponse.body?.string())

            subResponse.close() // Always close the response when not needed

            val movieId = subDoc.getElementById("movie-info").attr("data-movie-id").toString().toInt()
            var imdbCode = ""
            var rating = 0.0
            subDoc.getElementsByClass("rating-row").forEach { row ->
                if (row.hasAttr("itemscope")) {
                    imdbCode = row.getElementsByClass("icon")[0].attr("href").toString().split("/")[4]
                    row.allElements.forEach { element ->
                        if (element.hasAttr("itemprop") && element.attr("itemprop").toString() == "ratingValue") {
                            rating = element.ownText().toDouble()
                        }
                    }
                }
            }

            var title = ""
            var year = 0
            var bannerUrl = ""
            var runtime = 0

            subDoc.getElementById("mobile-movie-info").allElements.forEach {
                if (it.hasAttr("itemprop"))
                    title = it.ownText()
                else
                    if (it.ownText().isNotBlank() && it.ownText().isDigitsOnly())
                        year = it.ownText().toInt()
            }

            subDoc.getElementById("movie-poster").allElements.forEach {
                if (it.hasAttr("itemprop"))
                    bannerUrl = it.attr("src").toString()
            }

            subDoc.getElementsByClass("icon-clock")[0]?.let {
                val runtimeString = it.parent().ownText().trim()
                if (runtimeString.contains("hr")) {
                    runtime = runtimeString.split("hr")[0].trim().toInt() * 60
                    if (runtimeString.contains("min"))
                        runtime += runtimeString.split(" ")[2].trim().toInt()
                    return@let
                }
                if (runtimeString.contains("min"))
                    runtime += runtimeString.split("min")[0].trim().toInt()
            }

            return ItemModel(
                source = this,
                imageUrl = bannerUrl,
                url = url,
                title = title,
                description = ""
            ).apply { extras["info"] = service.getMovie(YTSQuery.MovieBuilder().setMovieId(movieId).build()).blockingGet()?.data?.movie.toJson() }
        } catch (e: Exception) {
            return null
        }
    }

    override fun getRecent(page: Int): Single<List<ItemModel>> =
        service.listMovies(YTSQuery.ListMoviesBuilder.getDefault().setPage(page).build())
            .map { it.data?.movies?.map(movieToModel) }

    override fun getList(page: Int): Single<List<ItemModel>> =
        service.listMovies(YTSQuery.ListMoviesBuilder.getDefault().setSortBy(YTSQuery.SortBy.rating).setPage(page).build())
            .map { it.data?.movies?.map(movieToModel) }

    override fun getItemInfo(model: ItemModel): Single<InfoModel> = Single.create { emitter ->
        model.extras["info"].toString().fromJson<Movies>()?.let {
            emitter.onSuccess(
                InfoModel(
                    source = this,
                    title = model.title,
                    url = model.url,
                    alternativeNames = emptyList(),
                    description = model.description,
                    imageUrl = model.imageUrl,
                    genres = it.genres.orEmpty(),
                    chapters = it.torrents?.map { t ->
                        ChapterModel("${model.title} - ${t.quality}", t.url.orEmpty(), "${t.date_uploaded}\n${t.size}", this)
                            .apply {
                                extras["hash"] = t.hash.orEmpty()
                                extras["torrents"] = t.toJson()
                                extras["info"] = it.toJson()
                            }
                    }.orEmpty()
                )
            )
        } ?: emitter.onError(Exception("Something went wrong"))
    }

    override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> = Single.create {
        it.onSuccess(listOf(Storage(link = chapterModel.url, source = chapterModel.url, quality = chapterModel.name, sub = "Yes")))
    }

    override fun searchList(searchText: CharSequence, page: Int, list: List<ItemModel>): Single<List<ItemModel>> =
        service.listMovies(YTSQuery.ListMoviesBuilder.getDefault().setQuery(searchText.toString()).setPage(page).build())
            .map { it.data?.movies?.map(movieToModel) }
}

object YtsService {
    fun build(): YTSApi = Retrofit.Builder()
        .baseUrl("https://yts.mx/api/v2/")
        .gsonConverter()
        .rx2FactoryAsync()
        .build()
        .create()
}

interface YTSApi {

    /** Get movie details by passing certain queries build using
     *  YTSQuery.MovieBuilder class*/

    @GET("movie_details.json")
    fun getMovie(@QueryMap params: Map<String, String>): Single<Base?>

    /** List movies by passing certain queries build using
     *  YTSQuery.ListMovieBuilder class*/

    @GET("list_movies.json")
    fun listMovies(@QueryMap params: Map<String, String>): Single<Base>

    /** A special callback for CustomMovieLayout
     */
    @GET("list_movies.json")
    fun listMovies(@QueryMap params: Map<String, String>, @Query("page") page: Int): Single<Base>
}

class YTSQuery {
    enum class Quality {
        q720p, q1080p, q2160p, q3D
    }

    enum class SortBy {
        title, year, rating, peers, seeds, download_count, like_count, date_added
    }

    enum class OrderBy {
        ascending, descending
    }

    enum class Genre {
        comedy, documentary, musical, sport, history, sci_fi, horror, romance, western, action, thriller, drama, mystery, crime, animation, adventure, fantasy, family
    }

    class MovieBuilder {
        var vals: MutableMap<String, String> = HashMap()

        /**
         * The ID of the movie.
         */
        fun setMovieId(id: Int): MovieBuilder {
            vals["movie_id"] = id.toString() + ""
            return this
        }

        /**
         * When set the data returned will include the added image URLs.
         *
         * Note: Response will not be successful sometimes when this is set to true
         */
        fun setIncludeImages(`val`: Boolean): MovieBuilder {
            vals["with_images"] = `val`.toString() + ""
            return this
        }

        /**
         * When set the data returned will include the added information about the cast.
         *
         * Note: Response will not be successful sometimes when this is set to true
         */
        fun setIncludeCast(`val`: Boolean): MovieBuilder {
            vals["with_cast"] = `val`.toString() + ""
            return this
        }

        /**
         * @return The queryMap used by Retrofit interface
         */
        fun build(): Map<String, String> {
            return vals
        }
    }

    class ListMoviesBuilder {
        companion object {
            fun getDefault(): ListMoviesBuilder =
                ListMoviesBuilder()
                    .setOrderBy(OrderBy.descending)
                    .setSortBy(SortBy.date_added)
                    .setPage(1)
                    .setLimit(20)
        }

        var vals: MutableMap<String, String> =
            HashMap()

        /**
         * Sorts the results by choosen value
         *
         * @String (title, year, rating, peers, seeds, download_count, like_count, date_added)
         * @default (date_added)
         */
        fun setSortBy(sortBy: SortBy): ListMoviesBuilder {
            vals["sort_by"] = sortBy.name
            return this
        }

        /**
         * Orders the results by either Ascending or Descending order
         *
         * @String (desc, asc)
         * @default (desc)
         */
        fun setOrderBy(orderBy: OrderBy): ListMoviesBuilder {
            var `val` = "asc"
            if (orderBy.name == "descending") `val` = "desc"
            vals["order_by"] = `val`
            return this
        }

        /**
         * Returns the list with the Rotten Tomatoes rating included
         *
         * @default (false)
         */
        fun setWithRtRatings(`val`: Boolean): ListMoviesBuilder {
            vals["with_rt_ratings"] = `val`.toString() + ""
            return this
        }

        /**
         * Used to filter by a given quality
         *
         * @String (720p, 1080p, 2160p, 3D)
         * @default (All)
         */
        fun setQuality(quality: Quality?): ListMoviesBuilder {
            var `val` = "720p"
            when (quality) {
                Quality.q3D -> `val` = "3D"
                Quality.q720p -> `val` = "720p"
                Quality.q1080p -> `val` = "1080p"
                Quality.q2160p -> `val` = "2160p"
            }
            vals["quality"] = `val`
            return this
        }

        /**
         * Used to filter by a given genre (See http://www.imdb.com/genre for full list)
         *
         * @default (All)
         */
        fun setGenre(genre: Genre): ListMoviesBuilder {
            var `val` = genre.name
            if (`val` == "sci_fi") `val` = "sci-fi"
            vals["genre"] = `val`
            return this
        }

        /**
         * The limit of results per page that has been set
         *
         * @Integer between 1 - 50 (inclusive)
         * @default (20)
         */
        fun setLimit(`val`: Int): ListMoviesBuilder {
            vals["limit"] = `val`.toString() + ""
            return this
        }

        /**
         * Used to see the next page of movies, eg limit=15 and page=2 will show you movies 15-30
         *
         * @default (1)
         */
        fun setPage(`val`: Int): ListMoviesBuilder {
            vals["page"] = `val`.toString() + ""
            return this
        }

        /**
         * Used to filter movie by a given minimum IMDb rating
         *
         * @Integer between 0 - 9 (inclusive)
         * @default (0)
         */
        fun setMinimumRating(`val`: Int): ListMoviesBuilder {
            vals["minimum_rating"] = `val`.toString() + ""
            return this
        }

        /**
         * Used for movie search, matching on: Movie Title/IMDb Code,
         * Actor Name/IMDb Code, Director Name/IMDb Code
         *
         * @default (0)
         */
        fun setQuery(`val`: String): ListMoviesBuilder {
            vals["query_term"] = `val` + ""
            return this
        }

        /**
         * @return The queryMap used by Retrofit interface
         */
        fun build(): Map<String, String> {
            return vals
        }
    }
}

// result generated from /json

//data class @meta(val server_time: Number?, val server_timezone: String?, val api_version: Number?, val execution_time: String?)

data class Base(val status: String?, val status_message: String?, val data: Data?, val meta: String)

data class Data(val movie_count: Number?, val limit: Number?, val page_number: Number?, val movies: List<Movies>?, val movie: Movies?)

data class Movies(
    val id: Number?,
    val url: String?,
    val imdb_code: String?,
    val title: String?,
    val title_english: String?,
    val title_long: String?,
    val slug: String?,
    val year: Number?,
    val rating: Number?,
    val runtime: Number?,
    val genres: List<String>?,
    val summary: String?,
    val description_full: String?,
    val synopsis: String?,
    val yt_trailer_code: String?,
    val language: String?,
    val mpa_rating: String?,
    val background_image: String?,
    val background_image_original: String?,
    val small_cover_image: String?,
    val medium_cover_image: String?,
    val large_cover_image: String?,
    val state: String?,
    val torrents: List<Torrents>?,
    val date_uploaded: String?,
    val date_uploaded_unix: Number?
)

data class Torrents(
    val url: String?,
    val hash: String?,
    val quality: String?,
    val type: String?,
    val seeds: Number?,
    val peers: Number?,
    val size: String?,
    val size_bytes: Number?,
    val date_uploaded: String?,
    val date_uploaded_unix: Number?
)