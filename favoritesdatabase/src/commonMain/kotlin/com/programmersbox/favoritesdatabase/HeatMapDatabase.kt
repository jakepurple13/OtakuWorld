package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Database(
    entities = [HeatMapItem::class],
    version = 2,
)
@ColumnTypeConverters(HeatMapConverter::class)
@ConstructedBy(HeatMapDatabaseConstructor::class)
abstract class HeatMapDatabase : RoomDatabase() {

    abstract fun heatMapDao(): HeatMapDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `supabase_id` TEXT DEFAULT ''")
                connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `HeatMapItem` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(databaseBuilder: DatabaseBuilder): HeatMapDatabase = databaseBuilder
            .build<HeatMapDatabase>("heatmap.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

}

@Dao
interface HeatMapDao {

    @Query("SELECT * FROM HeatMapItem ORDER BY time DESC")
    fun getAllHeatMaps(): Flow<List<HeatMapItem>>

    @Query("SELECT * FROM HeatMapItem ORDER BY time DESC")
    suspend fun getAllHeatMapsSync(): List<HeatMapItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeatMap(model: HeatMapItem)

    @Delete
    suspend fun deleteHeatMap(model: HeatMapItem)

    // New upsert function
    @OptIn(ExperimentalTime::class)
    @Transaction
    suspend fun upsertHeatMap(date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date) {
        val existingItem = getHeatMapByDate(date)
        if (existingItem != null) {
            // Item exists, update the count
            val updatedCount = existingItem.count + 1
            insertHeatMap(existingItem.copy(count = updatedCount)) // Use insert with REPLACE for update
        } else {
            // Item does not exist, insert new
            insertHeatMap(HeatMapItem(time = date, count = 1))
        }
    }

    @Query("SELECT * FROM HeatMapItem WHERE time = :date LIMIT 1")
    suspend fun getHeatMapByDate(date: LocalDate): HeatMapItem?

    @Query("SELECT * FROM HeatMapItem WHERE is_dirty = 1")
    suspend fun getDirtyHeatMapItems(): List<HeatMapItem>

    @Query("SELECT COUNT(*) FROM HeatMapItem WHERE is_dirty = 1")
    fun observeDirtyHeatMapCount(): Flow<Int>

    @Query("SELECT * FROM HeatMapItem WHERE time = :time LIMIT 1")
    suspend fun getHeatMapItemByTime(time: LocalDate): HeatMapItem?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateHeatMapItem(item: HeatMapItem)

    @Query("UPDATE HeatMapItem SET is_deleted = 1, is_dirty = 1, updated_at = :timestamp WHERE time = :time")
    suspend fun softDeleteHeatMapItem(time: LocalDate, timestamp: Long)

    @Query("UPDATE HeatMapItem SET updated_at = :timestamp, is_dirty = 0 WHERE time = :time")
    suspend fun markHeatMapItemSynced(time: LocalDate, timestamp: Long)

    @Query("UPDATE HeatMapItem SET is_deleted = 0")
    suspend fun resetAllHeatMapIsDeleted()

    @Query("DELETE FROM HeatMapItem WHERE is_deleted = 1")
    suspend fun deleteAllDeletedHeatMapItems()

    @Ignore
    fun getDailyAverage() = getAllHeatMaps()
        .map { heatMaps ->
            if (heatMaps.isEmpty()) return@map 0

            // FIX 1: Find the actual minimum (oldest) date, regardless of list order
            val firstReadingDate = heatMaps.minByOrNull { it.time }?.time
            val totalReadCount = heatMaps.sumOf { it.count }

            if (firstReadingDate != null) {
                val today = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .toEpochDays()

                val firstDay = firstReadingDate.toEpochDays()

                // Ensure we don't accidentally get a 0 or negative denominator if time zones shift
                val daysSinceFirstReading = maxOf(1, (today - firstDay) + 1)

                // FIX 2: Convert to Float/Double BEFORE dividing to prevent integer truncation
                (totalReadCount.toFloat() / daysSinceFirstReading).roundToInt()
            } else {
                0
            }
        }

    @Query("SELECT * FROM HeatMapItem WHERE is_deleted = 0 ORDER BY day_count DESC LIMIT 1")
    fun getHighestActiveCountItem(): Flow<HeatMapItem?>
}

@Serializable
@Entity(tableName = "HeatMapItem")
data class HeatMapItem(
    @PrimaryKey
    @ColumnInfo(name = "time")
    val time: LocalDate,
    @ColumnInfo(name = "day_count")
    val count: Int,
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)

class HeatMapConverter {
    @ColumnTypeConverter
    fun dateConverter(date: LocalDate) = Json.encodeToString(date)

    @ColumnTypeConverter
    fun stringConverter(string: String) = Json.decodeFromString<LocalDate>(string)
}