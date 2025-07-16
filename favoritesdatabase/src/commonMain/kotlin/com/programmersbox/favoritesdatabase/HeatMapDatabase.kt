package com.programmersbox.favoritesdatabase

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Database(
    entities = [HeatMapItem::class],
    version = 1,
)
@TypeConverters(HeatMapConverter::class)
abstract class HeatMapDatabase : RoomDatabase() {

    abstract fun heatMapDao(): HeatMapDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): HeatMapDatabase = databaseBuilder
            .build<HeatMapDatabase>("heatmap.db")
            .build()
    }

}

@Dao
interface HeatMapDao {

    @Query("SELECT * FROM HeatMapItem ORDER BY time DESC")
    fun getAllHeatMaps(): Flow<List<HeatMapItem>>

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

@Entity(tableName = "HeatMapItem")
data class HeatMapItem(
    @PrimaryKey
    @ColumnInfo(name = "time")
    val time: LocalDate,
    @ColumnInfo(name = "day_count")
    val count: Int
)

class HeatMapConverter {
    @TypeConverter
    fun dateConverter(date: LocalDate) = Json.encodeToString(date)

    @TypeConverter
    fun stringConverter(string: String) = Json.decodeFromString<LocalDate>(string)
}