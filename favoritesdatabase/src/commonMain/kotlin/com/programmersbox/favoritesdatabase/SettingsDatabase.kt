package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "ActivityTable")
data class ActivityTable(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "cumulative_seconds", defaultValue = "0") val cumulativeSeconds: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_dirty", defaultValue = "0") val isDirty: Boolean = false,
)

@Dao
interface ActivityDao {

    @Query("SELECT * FROM ActivityTable WHERE id = 1")
    suspend fun getActivity(): ActivityTable?

    @Query("SELECT * FROM ActivityTable WHERE id = 1")
    fun observeActivity(): Flow<ActivityTable?>

    @Query(
        "INSERT INTO ActivityTable (id, cumulative_seconds) VALUES (1, :seconds) " +
            "ON CONFLICT(id) DO UPDATE SET cumulative_seconds = cumulative_seconds + :seconds"
    )
    suspend fun incrementSeconds(seconds: Long = 1L)

    @Query("UPDATE ActivityTable SET is_dirty = 1, updated_at = :timestamp WHERE id = 1")
    suspend fun markDirtyNow(timestamp: Long)

    @Query(
        "INSERT INTO ActivityTable (id, cumulative_seconds, updated_at, is_dirty) " +
            "VALUES (1, :seconds, :timestamp, 0) " +
            "ON CONFLICT(id) DO UPDATE SET cumulative_seconds = :seconds, updated_at = :timestamp, is_dirty = 0"
    )
    suspend fun upsertSynced(seconds: Long, timestamp: Long)
}

@Database(entities = [ActivityTable::class], version = 1)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): SettingsDatabase = databaseBuilder
            .build<SettingsDatabase>("settings_database.db")
            .build()
    }
}
