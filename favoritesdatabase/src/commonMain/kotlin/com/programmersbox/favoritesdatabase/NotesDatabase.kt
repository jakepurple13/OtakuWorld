package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Entity(tableName = "notes")
@Serializable
data class NoteItem(
    @PrimaryKey
    @ColumnInfo(name = "itemUrl")
    val itemUrl: String,
    @ColumnInfo(name = "itemTitle")
    val itemTitle: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
)

@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteItem::class)
data class NoteItemFts(
    val content: String,
    val itemTitle: String,
)

@Dao
interface NotesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteItem)

    @Query("DELETE FROM notes WHERE itemUrl = :itemUrl")
    suspend fun deleteNote(itemUrl: String)

    @Query("SELECT * FROM notes WHERE itemUrl = :itemUrl")
    fun getNote(itemUrl: String): Flow<NoteItem?>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteItem>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<NoteItem>

    @Query("SELECT COUNT(itemUrl) FROM notes")
    fun getAllNotesCount(): Flow<Int>

    @Query("""
        SELECT * FROM notes WHERE rowid IN (
            SELECT rowid FROM notes_fts
            WHERE notes_fts MATCH :query
        ) ORDER BY timestamp DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteItem>>
}

@Database(
    entities = [NoteItem::class, NoteItemFts::class],
    version = 1,
    exportSchema = true,
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {
        fun getInstance(databaseBuilder: DatabaseBuilder): NotesDatabase =
            databaseBuilder
                .build<NotesDatabase>("notes.db")
                .build()
    }
}
