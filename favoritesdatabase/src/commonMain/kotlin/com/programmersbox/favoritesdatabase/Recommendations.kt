package com.programmersbox.favoritesdatabase

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
}

class Converters {
    @TypeConverter
    fun fromList(value: List<String>) = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String) = Json.decodeFromString<List<String>>(value)
}

@Serializable
data class RecommendationResponse(
    val response: String? = null,
    val recommendations: List<Recommendation> = emptyList(),
)

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