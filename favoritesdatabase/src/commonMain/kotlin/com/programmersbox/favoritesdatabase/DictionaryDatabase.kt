package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Entity(tableName = "dictionary_entries")
@Serializable
data class DictionaryEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "term")
    val term: String,
    @ColumnInfo(name = "definition")
    val definition: String? = null,
    @ColumnInfo(name = "reading")
    val reading: String? = null,
    @ColumnInfo(name = "category")
    val category: String? = null,
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    @ColumnInfo(name = "language")
    val language: String? = null,
    @ColumnInfo(name = "dateAdded")
    val dateAdded: Long = Clock.System.now().toEpochMilliseconds(),
)

@Dao
interface DictionaryDao {
    @Insert
    suspend fun insert(entry: DictionaryEntry): Long

    @Update
    suspend fun update(entry: DictionaryEntry)

    @Delete
    suspend fun delete(entry: DictionaryEntry)

    @Query("SELECT * FROM dictionary_entries WHERE id = :id")
    fun getById(id: Long): Flow<DictionaryEntry?>

    @Query("SELECT * FROM dictionary_entries ORDER BY term COLLATE NOCASE ASC")
    fun getAllByTerm(): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary_entries")
    suspend fun getAllSync(): List<DictionaryEntry>

    @Query("SELECT * FROM dictionary_entries ORDER BY dateAdded DESC")
    fun getAllByDateAdded(): Flow<List<DictionaryEntry>>

    @Query("SELECT * FROM dictionary_entries ORDER BY category COLLATE NOCASE ASC, term COLLATE NOCASE ASC")
    fun getAllByCategory(): Flow<List<DictionaryEntry>>

    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE term LIKE '%' || :query || '%'
           OR definition LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
        ORDER BY term COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<DictionaryEntry>>

    @Query("SELECT COUNT(id) FROM dictionary_entries")
    fun getCount(): Flow<Int>
}

@Database(
    entities = [DictionaryEntry::class],
    version = 1,
    exportSchema = true,
)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): DictionaryDatabase =
            databaseBuilder
                .build<DictionaryDatabase>("dictionary_database")
                .build()
    }
}
