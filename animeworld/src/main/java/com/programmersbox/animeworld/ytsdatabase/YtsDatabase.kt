package com.programmersbox.animeworld.ytsdatabase

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.kpstv.bindings.AutoGenerateConverter
import com.kpstv.bindings.ConverterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.lang.reflect.Type


@AutoGenerateConverter(using = ConverterType.GSON)
data class Torrent(
    var title: String, var banner_url: String,
    val url: String, val hash: String, val quality: String,
    val type: String, val seeds: Int, val peers: Int,
    @SerializedName("size") val size_pretty: String,
    @SerializedName("size_bytes") val size: Long,
    val date_uploaded: String, val date_uploaded_unix: String,
    var movieId: Int, var imdbCode: String
) : Serializable

@AutoGenerateConverter(using = ConverterType.GSON)
data class TorrentJob(
    val title: String,
    val bannerUrl: String,
    val progress: Int,
    val seeds: Int,
    val downloadSpeed: Float,
    val currentSize: Long,
    val totalSize: Long? = null,
    val isPlay: Boolean,
    var status: String,
    val peers: Int,
    val magnetHash: String
) : Serializable {
    companion object {
        fun from(model: Torrent, status: String = "Paused") =
            TorrentJob(
                title = model.title,
                bannerUrl = model.banner_url,
                progress = 0,
                seeds = model.seeds,
                downloadSpeed = 0f,
                currentSize = 0,
                totalSize = model.size,
                isPlay = false,
                status = status,
                peers = model.peers,
                magnetHash = model.hash
            )
    }
}

object TorrentListConverter {
    @TypeConverter
    @JvmStatic
    fun fromTorrentListToString(torrents: ArrayList<Torrent?>?): String? {
        if (torrents == null) {
            return null
        }
        val gson = Gson()
        val type: Type = object : TypeToken<ArrayList<Torrent?>?>() {}.type
        return gson.toJson(torrents, type)
    }

    @TypeConverter
    @JvmStatic
    fun toTorrentListfromString(torrentString: String?): ArrayList<Torrent>? {
        if (torrentString == null) {
            return null
        }
        val gson = Gson()
        val type: Type = object : TypeToken<ArrayList<Torrent?>?>() {}.type
        return gson.fromJson<ArrayList<Torrent>>(torrentString, type)
    }
}

@Database(entities = [Model.response_download::class, Model.response_pause::class], version = 2)
@TypeConverters(
    TorrentListConverter::class,
    TorrentJobConverter::class,
    TorrentConverter::class
)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun getDownloadDao(): DownloadDao
    abstract fun getPauseDao(): PauseDao

    companion object {

        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        fun getInstance(context: Context): DownloadDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(context).also { INSTANCE = it } }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, DownloadDatabase::class.java, "downloads.db").build()

    }

}


@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(data: Model.response_download)

    @Query("select * from table_download where hash = :hash")
    fun getDownload(hash: String): Model.response_download?

    @Query("update table_download set recentlyPlayed = :updateRecentlyPlayed, lastSavedPosition = :updateLastPosition where hash = :hash")
    fun updateDownload(hash: String, updateRecentlyPlayed: Boolean, updateLastPosition: Int)

    @Query("update table_download set recentlyPlayed = :updateRecentlyPlayed where hash = :hash")
    suspend fun updateDownload(hash: String, updateRecentlyPlayed: Boolean)

    @Delete
    fun delete(data: Model.response_download)

    @Query("select * from table_download")
    fun getAllLiveDownloads(): LiveData<List<Model.response_download>>

    @Query("select * from table_download")
    fun getAllDownloads(): List<Model.response_download>
}

class PauseRepository constructor(
    private val pauseDao: PauseDao
) {
    private suspend fun getPauseModelByQuery(hash: String): Model.response_pause? {
        return withContext(Dispatchers.IO) {
            pauseDao.getTorrentJob(hash)
        }
    }

    fun savePauseModel(data: Model.response_pause) {
        GlobalScope.launch(Dispatchers.IO) {
            if (pauseDao.getTorrentJob(data.hash) == null)
                pauseDao.upsert(data)
        }
    }

    fun deletePause(hash: String) {
        GlobalScope.launch(Dispatchers.IO) {
            getPauseModelByQuery(hash)?.let {
                pauseDao.delete(it)
            }
        }
    }

    fun getAllPauseJob(): LiveData<List<Model.response_pause>> {
        return pauseDao.getAllData()
    }
}

class DownloadRepository constructor(
    private val downloadDao: DownloadDao
) {
    private suspend fun getDownload(hash: String): Model.response_download? {
        return withContext(Dispatchers.IO) {
            downloadDao.getDownload(hash)
        }
    }

    private val lock = Any()
    fun saveDownload(data: Model.response_download) {
        GlobalScope.launch(Dispatchers.IO) {
            synchronized(lock) {
                if (downloadDao.getDownload(data.hash) == null)
                    downloadDao.upsert(data)
            }
        }
    }

    fun updateDownload(hash: String, recentlyPlayed: Boolean, lastPosition: Int) = GlobalScope.launch(Dispatchers.IO) {
        downloadDao.updateDownload(hash, recentlyPlayed, lastPosition)
    }

    fun deleteDownload(hash: String) {
        GlobalScope.launch(Dispatchers.IO) {
            getDownload(hash)?.let {
                downloadDao.delete(it)
            }
        }
    }

    suspend fun updateAllNormalDownloads() {
        downloadDao.getAllDownloads().forEach {
            downloadDao.updateDownload(it.hash, false)
        }
    }

    fun getAllDownloads(): LiveData<List<Model.response_download>> {
        return downloadDao.getAllLiveDownloads()
    }

}

@Dao
interface PauseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(data: Model.response_pause)

    @Query("select * from table_pause where hash = :hash")
    fun getTorrentJob(hash: String): Model.response_pause?

    @Delete
    fun delete(data: Model.response_pause)

    @Query("select * from table_pause")
    fun getAllData(): LiveData<List<Model.response_pause>>
}

object Model {
    data class response_movie(val status: String, val status_message: String, val data: Any)

    @Entity(tableName = "table_favourites")
    data class response_favourite(
        @PrimaryKey(autoGenerate = true)
        val id: Int? = null,
        val movieId: Int,
        val imdbCode: String,
        val title: String,
        val imageUrl: String,
        val rating: Double,
        val runtime: Int,
        val year: Int
    ) {
        /*companion object {
            fun from(movie: MovieShort) =
                response_favourite(
                    movieId = movie.movieId!!,
                    year = movie.year!!,
                    rating = movie.rating,
                    runtime = movie.runtime,
                    imageUrl = movie.bannerUrl,
                    title = movie.title,
                    imdbCode = movie.imdbCode!!
                )
        }*/
    }

    @Entity(tableName = "table_pause")
    data class response_pause(
        @PrimaryKey(autoGenerate = true)
        val id: Int? = null,
        val job: TorrentJob,
        val hash: String,
        val torrent: Torrent?,
        val saveLocation: String?
    ) : Serializable

    @Entity(tableName = "table_download")
    data class response_download(
        @PrimaryKey(autoGenerate = true)
        val id: Int? = null,
        val movieId: Int?,
        val imdbCode: String?,
        val title: String,
        val imagePath: String?,
        val downloadPath: String?,
        val size: Long,
        val date_downloaded: String?,
        val total_video_length: Long,
        val hash: String,
        val videoPath: String?,
        @ColumnInfo(name = "recentlyPlayed") val recentlyPlayed: Boolean = false,
        @ColumnInfo(name = "lastSavedPosition") val lastSavedPosition: Int = 0
    ) : Serializable

    data class response_cast_movie(
        val cast: List<Cast>,
        val id: Int
    ) {
        data class Cast(
            val adult: Boolean,
            @SerializedName("backdrop_path")
            val backdropPath: String?,
            val character: String,
            @SerializedName("credit_id")
            val creditId: String,
            @SerializedName("genre_ids")
            val genreIds: List<Int>,
            val id: Int,
            @SerializedName("original_language")
            val originalLanguage: String,
            @SerializedName("original_title")
            val originalTitle: String,
            val overview: String,
            val popularity: Double,
            @SerializedName("poster_path")
            val posterPath: String?,
            @SerializedName("release_date")
            val releaseDate: String?,
            val title: String,
            val video: Boolean,
            @SerializedName("vote_average")
            val voteAverage: Double,
            @SerializedName("vote_count")
            val voteCount: Int
        ) {
            fun getPosterImage(): String = ""//""${AppInterface.TMDB_IMAGE_PREFIX}${posterPath}"
        }
    }
}
