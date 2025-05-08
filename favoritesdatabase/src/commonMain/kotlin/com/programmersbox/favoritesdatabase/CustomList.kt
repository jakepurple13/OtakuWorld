@file:OptIn(ExperimentalTime::class)

package com.programmersbox.favoritesdatabase

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
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
    version = 10,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 7),
        AutoMigration(from = 7, to = 8),
    ]
)
abstract class ListDatabase : RoomDatabase() {

    abstract fun listDao(): ListDao

    companion object {

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(connection: SQLiteConnection) {
                //change the uuid of CustomListInfo to text
                connection.execSQL("DROP TABLE IF EXISTS CustomListInfo")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(connection: SQLiteConnection) {
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

        fun getInstance(databaseBuilder: DatabaseBuilder): ListDatabase = databaseBuilder
            .build<ListDatabase>("list.db")
            .fallbackToDestructiveMigration(true)
            .addMigrations(
                MIGRATION_8_9,
                MIGRATION_9_10
            )
            .build()
    }
}

@Dao
interface ListDao {

    @Transaction
    @Query("SELECT * FROM CustomListItem ORDER BY time DESC")
    fun getAllLists(): Flow<List<CustomList>>

    @Query("SELECT COUNT(uuid) FROM CustomListItem")
    fun getAllListsCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM CustomListItem ORDER BY time DESC")
    suspend fun getAllListsSync(): List<CustomList>

    @Transaction
    @Query("SELECT * FROM CustomListItem WHERE :uuid = uuid")
    suspend fun getCustomListItem(uuid: String): CustomList

    @Transaction
    @Query("SELECT * FROM CustomListItem WHERE :uuid = uuid")
    fun getCustomListItemFlow(uuid: String): Flow<CustomList>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createList(listItem: CustomListItem): Long

    @Insert
    suspend fun addItem(listItem: CustomListInfo)

    @Delete
    suspend fun removeItem(listItem: CustomListInfo)

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
}

@Serializable
data class CustomList(
    @Embedded
    val item: CustomListItem,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "uuid"
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
)
