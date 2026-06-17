package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@TypeConverters(Converters::class)
@Database(
    entities = [Recommendation::class],
    version = 2,
)
abstract class RecommendationDatabase : RoomDatabase() {

    abstract fun recommendationDao(): RecommendationDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `Recommendation` ADD COLUMN `supabase_id` TEXT NOT NULL DEFAULT ''")
                connection.execSQL("ALTER TABLE `Recommendation` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `Recommendation` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `Recommendation` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `Recommendation` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(databaseBuilder: DatabaseBuilder): RecommendationDatabase = databaseBuilder
            .build<RecommendationDatabase>("recommendations.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM Recommendation")
    fun getAllRecommendations(): Flow<List<Recommendation>>

    @Query("SELECT * FROM Recommendation")
    suspend fun getAllRecommendationsSync(): List<Recommendation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(recommendation: Recommendation)

    @Query("DELETE FROM Recommendation WHERE title = :id")
    suspend fun deleteRecommendation(id: String)

    @Delete
    suspend fun deleteRecommendation(id: Recommendation)

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
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)