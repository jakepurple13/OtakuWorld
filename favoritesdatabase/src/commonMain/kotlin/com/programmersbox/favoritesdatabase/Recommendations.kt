package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@TypeConverters(Converters::class)
@Database(
    entities = [Recommendation::class],
    version = 1,
)
abstract class RecommendationDatabase : RoomDatabase() {

    abstract fun recommendationDao(): RecommendationDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): RecommendationDatabase = databaseBuilder
            .build<RecommendationDatabase>("recommendations.db")
            .build()
    }
}

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM Recommendation")
    fun getAllRecommendations(): Flow<List<Recommendation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(recommendation: Recommendation)

    @Query("DELETE FROM Recommendation WHERE title = :id")
    suspend fun deleteRecommendation(id: String)

    @Query("SELECT COUNT(*) FROM Recommendation")
    fun getRecommendationCount(): Flow<Int>
}

class Converters {
    @TypeConverter
    fun fromList(value: List<String>) = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String) = Json.decodeFromString<List<String>>(value)
}

@Entity("Recommendation")
@Serializable
data class Recommendation(
    @PrimaryKey
    @ColumnInfo("title")
    val title: String,
    @ColumnInfo("description")
    val description: String,
    @ColumnInfo("reason")
    val reason: String,
    @ColumnInfo("genre")
    val genre: List<String>,
)