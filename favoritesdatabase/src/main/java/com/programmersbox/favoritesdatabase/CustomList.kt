package com.programmersbox.favoritesdatabase

import android.content.Context
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
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Database(
    entities = [CustomListItem::class, CustomListInfo::class],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 7),
    ]
)
abstract class ListDatabase : RoomDatabase() {

    abstract fun listDao(): ListDao

    companion object {

        @Volatile
        private var INSTANCE: ListDatabase? = null

        fun getInstance(context: Context): ListDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(context).also { INSTANCE = it } }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, ListDatabase::class.java, "list.db")
                .build()
    }

}

@Dao
interface ListDao {

    @Transaction
    @Query("SELECT * FROM CustomListItem ORDER BY time DESC")
    fun getAllLists(): Flow<List<CustomList>>

    @Transaction
    @Query("SELECT * FROM CustomListItem WHERE :uuid = uuid")
    suspend fun getCustomListItem(uuid: UUID): CustomList

    @Transaction
    @Query("SELECT * FROM CustomListItem WHERE :uuid = uuid")
    fun getCustomListItemFlow(uuid: UUID): Flow<CustomList>

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

    @Ignore
    suspend fun create(name: String) {
        createList(
            CustomListItem(
                uuid = UUID.randomUUID(),
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
        updateList(item.copy(time = System.currentTimeMillis()))
    }

    @Ignore
    suspend fun addToList(uuid: UUID, title: String, description: String, url: String, imageUrl: String, source: String): Boolean {
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
    suspend fun updateBiometric(uuid: UUID, useBiometric: Boolean)
}

data class CustomList(
    @Embedded
    val item: CustomListItem,
    @Relation(
        parentColumn = "uuid",
        entityColumn = "uuid"
    )
    val list: List<CustomListInfo>,
)

@Entity(tableName = "CustomListItem")
data class CustomListItem(
    @PrimaryKey
    @ColumnInfo(name = "uuid")
    val uuid: UUID,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "time")
    val time: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val useBiometric: Boolean = false,
)

@Entity(tableName = "CustomListInfo")
data class CustomListInfo(
    @PrimaryKey
    @ColumnInfo(defaultValue = "0c65586e-f3dc-4878-be63-b134fb46466c")
    val uniqueId: String = UUID.randomUUID().toString(),
    @ColumnInfo("uuid")
    val uuid: UUID,
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
