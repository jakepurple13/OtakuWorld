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
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [BlurHashItem::class],
    version = 2,
)
abstract class BlurHashDatabase : RoomDatabase() {

    abstract fun blurDao(): BlurHashDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `supabase_id` TEXT NOT NULL DEFAULT ''")
                connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `BlurHashItem` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(databaseBuilder: DatabaseBuilder): BlurHashDatabase = databaseBuilder
            .build<BlurHashDatabase>("blurhash.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}

@Dao
interface BlurHashDao {
    @Query("SELECT * FROM BlurHashItem")
    fun getAllHashes(): Flow<List<BlurHashItem>>

    @Query("SELECT COUNT(url) FROM BlurHashItem")
    fun getAllHashesCount(): Flow<Int>

    @Query("SELECT * FROM BlurHashItem WHERE url=:url")
    fun getHash(url: String?): Flow<BlurHashItem?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHash(model: BlurHashItem)

    @Delete
    suspend fun deleteHash(model: BlurHashItem)
}

@Entity(tableName = "BlurHashItem")
data class BlurHashItem(
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "blur_hash")
    val blurHash: String,
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)