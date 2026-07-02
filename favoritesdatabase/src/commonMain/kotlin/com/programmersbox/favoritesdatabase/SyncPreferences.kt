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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "backup_preferences")
data class BackupPreferenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "table_name")
    val tableName: String,
    @ColumnInfo(name = "enabled", defaultValue = "1")
    val enabled: Boolean = true,
)

@Dao
interface BackupPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreference(preference: BackupPreferenceEntity)

    @Query("SELECT * FROM backup_preferences WHERE table_name = :tableName")
    suspend fun getPreference(tableName: String): BackupPreferenceEntity?

    @Query("SELECT * FROM backup_preferences")
    fun observeAllPreferences(): Flow<List<BackupPreferenceEntity>>
}

@Database(
    entities = [BackupPreferenceEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SyncPreferences : RoomDatabase() {
    abstract fun backupPreferenceDao(): BackupPreferenceDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): SyncPreferences =
            databaseBuilder
                .build<SyncPreferences>("sync_preferences.db")
                .build()
    }
}
