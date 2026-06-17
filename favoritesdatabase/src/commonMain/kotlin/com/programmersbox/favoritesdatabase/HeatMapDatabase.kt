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
import androidx.room3.Transaction
import androidx.room3.TypeConverter
import androidx.room3.TypeConverters
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Database(
    entities = [HeatMapItem::class],
    version = 2,
)
@TypeConverters(HeatMapConverter::class)
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
    @TypeConverter
    fun dateConverter(date: LocalDate) = Json.encodeToString(date)

    @TypeConverter
    fun stringConverter(string: String) = Json.decodeFromString<LocalDate>(string)
}