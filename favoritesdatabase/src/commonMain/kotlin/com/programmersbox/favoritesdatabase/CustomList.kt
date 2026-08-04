@file:OptIn(ExperimentalTime::class)

package com.programmersbox.favoritesdatabase

import androidx.room3.AutoMigration
import androidx.room3.ColumnInfo
import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Relation
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Database(
    entities = [CustomListItem::class, CustomListInfo::class],
    version = 12,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 10, to = 11),
    ]
)
@ConstructedBy(ListDatabaseConstructor::class)
abstract class ListDatabase : RoomDatabase() {

    abstract fun listDao(): ListDao

    companion object {

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override suspend fun migrate(connection: SQLiteConnection) {
                //change the uuid of CustomListInfo to text
                connection.execSQL("DROP TABLE IF EXISTS CustomListInfo")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override suspend fun migrate(connection: SQLiteConnection) {
                //change the uuid of CustomListInfo to text
                connection.execSQL("DROP TABLE IF EXISTS CustomListInfo")
                connection.execSQL("DROP TABLE IF EXISTS CustomListItem")
                connection.execSQL(
                    """
                    CREATE TABLE CustomListInfo (
                        uniqueId TEXT PRIMARY KEY NOT NULL DEFAULT '0c65586e-f3dc-4878-be63-b134fb46466c',
                        uuid TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        url TEXT NOT NULL,
                        imageUrl TEXT NOT NULL,
                        sources TEXT NOT NULL
                    )
                """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE TABLE CustomListItem (
                        uuid TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        time INTEGER NOT NULL,
                        useBiometric INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override suspend fun migrate(connection: SQLiteConnection) {
                listOf("CustomListItem", "CustomListInfo").forEach { table ->
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `supabase_id` TEXT DEFAULT ''")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                    connection.execSQL("ALTER TABLE `$table` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
                }
            }
        }

        fun getInstance(databaseBuilder: DatabaseBuilder): ListDatabase = databaseBuilder
            .build<ListDatabase>("list.db")
            .fallbackToDestructiveMigration(true)
            .addMigrations(
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_11_12
            )
            .build()
    }
}

@Dao
interface ListDao {

    @Transaction
    @Query("SELECT * FROM CustomListItem WHERE is_deleted = 0 ORDER BY useBiometric ASC, time DESC ")
    fun getAllLists(): Flow<List<CustomList>>

    @Query("SELECT COUNT(uuid) FROM CustomListItem WHERE is_deleted = 0")
    fun getAllListsCount(): Flow<Int>

    @Query("SELECT COUNT(uuid) FROM CustomListInfo WHERE is_deleted = 0")
    fun getAllListItemsCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM CustomListItem WHERE is_deleted = 0 ORDER BY time DESC")
    suspend fun getAllListsSync(): List<CustomList>

    @Transaction
    @Query("SELECT * FROM CustomListItem WHERE :uuid = uuid")
    suspend fun getCustomListItem(uuid: String): CustomList

    @Transaction
    @Query("SELECT * FROM CustomListItem WHERE :uuid = uuid AND is_deleted = 0")
    fun getCustomListItemFlow(uuid: String): Flow<CustomList>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createList(listItem: CustomListItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItem(listItem: CustomListInfo)

    @Delete
    suspend fun removeItem(listItem: CustomListInfo)

    @Query("DELETE FROM CustomListInfo WHERE :uuid = uuid")
    suspend fun removeItem(uuid: String)

    @Update
    suspend fun updateList(listItem: CustomListItem)

    @Delete
    suspend fun removeList(item: CustomListItem)

    @OptIn(ExperimentalUuidApi::class)
    @Ignore
    suspend fun create(name: String) {
        createList(
            CustomListItem(
                uuid = Uuid.random().toString(),
                name = name,
            )
        )
    }

    @Ignore
    suspend fun removeList(item: CustomList) {
        item.list.forEach { removeItem(it) }
        removeList(item.item)
    }

    @Ignore
    suspend fun updateFullList(item: CustomListItem) {
        updateList(item.copy(time = Clock.System.now().toEpochMilliseconds()))
    }

    @Ignore
    suspend fun addToList(uuid: String, title: String, description: String, url: String, imageUrl: String, source: String): Boolean {
        val item = getCustomListItem(uuid)
        return if (item.list.any { it.url == url && it.uuid == uuid }) {
            false
        } else {
            addItem(CustomListInfo(uuid = uuid, title = title, description = description, url = url, imageUrl = imageUrl, source = source))
            updateFullList(item.item)
            true
        }
    }

    @Query("UPDATE CustomListItem SET useBiometric = :useBiometric WHERE uuid = :uuid")
    suspend fun updateBiometric(uuid: String, useBiometric: Boolean)

    @Query("SELECT * FROM CustomListItem WHERE is_dirty = 1")
    suspend fun getDirtyCustomListItems(): List<CustomListItem>

    @Query("SELECT COUNT(*) FROM CustomListItem WHERE is_dirty = 1")
    fun observeDirtyCustomListItemCount(): Flow<Int>

    @Query("SELECT * FROM CustomListInfo WHERE is_dirty = 1")
    suspend fun getDirtyCustomListInfo(): List<CustomListInfo>

    @Query("SELECT COUNT(*) FROM CustomListInfo WHERE is_dirty = 1")
    fun observeDirtyCustomListInfoCount(): Flow<Int>

    @Query("SELECT * FROM CustomListItem WHERE uuid = :uuid")
    suspend fun getCustomListItemByUuid(uuid: String): CustomListItem?

    @Query("SELECT * FROM CustomListInfo WHERE uniqueId = :uniqueId")
    suspend fun getCustomListInfoByUniqueId(uniqueId: String): CustomListInfo?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCustomListItem(item: CustomListItem)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateCustomListInfo(info: CustomListInfo)

    @Query("UPDATE CustomListItem SET is_deleted = 1, is_dirty = 1, updated_at = :timestamp WHERE uuid = :uuid")
    suspend fun softDeleteCustomListItem(uuid: String, timestamp: Long)

    @Query("UPDATE CustomListInfo SET is_deleted = 1, is_dirty = 1, updated_at = :timestamp WHERE uniqueId = :uniqueId")
    suspend fun softDeleteCustomListInfo(uniqueId: String, timestamp: Long)

    @Query("UPDATE CustomListItem SET updated_at = :timestamp, is_dirty = 0 WHERE uuid = :uuid")
    suspend fun markCustomListItemSynced(uuid: String, timestamp: Long)

    @Query("UPDATE CustomListInfo SET updated_at = :timestamp, is_dirty = 0 WHERE uniqueId = :uniqueId")
    suspend fun markCustomListInfoSynced(uniqueId: String, timestamp: Long)

    @Query("SELECT * FROM CustomListItem")
    suspend fun getAllCustomListItemsSync(): List<CustomListItem>

    @Query("UPDATE CustomListItem SET is_deleted = 0")
    suspend fun resetAllCustomListItemsIsDeleted()

    @Query("DELETE FROM CustomListItem WHERE is_deleted = 1")
    suspend fun deleteAllDeletedCustomListItems()

    @Query("SELECT * FROM CustomListInfo")
    suspend fun getAllCustomListInfoSync(): List<CustomListInfo>

    @Query("UPDATE CustomListInfo SET is_deleted = 0")
    suspend fun resetAllCustomListInfoIsDeleted()

    @Query("DELETE FROM CustomListInfo WHERE is_deleted = 1")
    suspend fun deleteAllDeletedCustomListInfo()
}

@Serializable
data class CustomList(
    @Embedded
    val item: CustomListItem,
    @Relation(
        parentColumns = ["uuid"],
        entityColumns = ["uuid"]
    )
    val list: List<CustomListInfo>,
)

@Serializable
@Entity(tableName = "CustomListItem")
data class CustomListItem(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "time")
    val time: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(defaultValue = "0")
    val useBiometric: Boolean = false,
    @ColumnInfo(name = "description", defaultValue = "")
    val description: String = "",
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
@Entity(tableName = "CustomListInfo")
data class CustomListInfo(
    @PrimaryKey
    @ColumnInfo(defaultValue = "0c65586e-f3dc-4878-be63-b134fb46466c")
    val uniqueId: String = Uuid.random().toString(),
    @ColumnInfo("uuid")
    val uuid: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "imageUrl")
    val imageUrl: String,
    @ColumnInfo(name = "sources")
    val source: String,
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)
