package com.programmersbox.koogintegration.customscraper.database

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

//TODO: Maybe database or new source of dynamically webscraped sources. We save the url, cover image, and name.
// If we go to see it, we must include refresh options since it could fail.
// Also refresh for cover image, chapter list, etc.
// MAYBE also look into a fine tuned model?
// Maybe a pure local model?
// Maybe give the user a choice on local or cloud.
// Might be a thing to not do the chapter extract? Maybe give the option?

@Entity(tableName = "scraper_sources")
@Serializable
data class ScraperSource(
    @PrimaryKey
    val url: String,
    val name: String,
    val coverImageUrl: String,
    val description: String,
)

@Entity(tableName = "scraper_fts")
@Fts4(contentEntity = ScraperSource::class)
data class ScraperSourceFts(
    val name: String,
    val description: String,
)

@Dao
interface ScraperDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScraper(scraperSource: ScraperSource)

    @Delete
    suspend fun deleteScraper(scraperSource: ScraperSource)

    @Query("SELECT * FROM scraper_sources")
    fun getAllScrapers(): Flow<List<ScraperSource>>

    @Query("SELECT * FROM scraper_sources WHERE url = :url")
    fun getScrapersForDetail(url: String): Flow<List<ScraperSource>>

    @Query("SELECT * FROM scraper_sources WHERE url = :url")
    fun getScraper(url: String): Flow<ScraperSource?>

    @Query(
        """
        SELECT * FROM scraper_sources WHERE rowid IN (
            SELECT rowid FROM scraper_fts
            WHERE scraper_fts MATCH :query
        )
    """
    )
    fun searchScrapers(query: String): Flow<List<ScraperSource>>

    @Query("SELECT * FROM scraper_sources")
    suspend fun getAllScrapersSync(): List<ScraperSource>

    @Query("SELECT COUNT(url) FROM scraper_sources")
    fun getAllScrapersCount(): Flow<Int>

    @Update
    suspend fun updateScraper(scraperSource: ScraperSource)
}

@Database(
    entities = [ScraperSource::class, ScraperSourceFts::class],
    version = 1,
    exportSchema = true,
)
abstract class ScraperDatabase : RoomDatabase() {
    abstract fun scraperDao(): ScraperDao

    companion object {
        fun getInstance(builder: Builder<ScraperDatabase>): ScraperDatabase = builder.build()
    }
}
