package com.programmersbox.favoritesdatabase

import androidx.room3.ColumnInfo
import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Update
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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
    @ColumnInfo(name = "supabase_id", defaultValue = "") val supabaseId: String? = null,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "is_dirty", defaultValue = "1") val isDirty: Boolean = true,
)

@Entity(tableName = "notes_fts")
@Fts5(
    contentEntity = NoteItem::class,
    tokenizerArgs = ["remove_diacritics", "2"],
    prefix = [2, 3],
)
data class NoteItemFts(
    val content: String,
    val itemTitle: String,
)

/**
 * One notes search result. The snippet marks matched words with `\u0002` (start) and
 * `\u0003` (end).
 */
data class NoteFtsHit(
    val itemUrl: String,
    val score: Double,
    val contentSnippet: String,
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

    // Callers restrict the match to the content column (see ftsMatch(column = "content")).
    @Query(
        """
        SELECT notes.itemUrl AS itemUrl,
               bm25(notes_fts) AS score,
               snippet(notes_fts, 0, char(2), char(3), '…', 12) AS contentSnippet
        FROM notes_fts JOIN notes ON notes.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :match AND notes.is_deleted = 0
        ORDER BY score
        """
    )
    fun searchNoteHits(match: String): Flow<List<NoteFtsHit>>

    @Query("SELECT * FROM notes WHERE is_dirty = 1")
    suspend fun getDirtyNotes(): List<NoteItem>

    @Query("SELECT COUNT(*) FROM notes WHERE is_dirty = 1")
    fun observeDirtyNoteCount(): Flow<Int>

    @Query("SELECT * FROM notes WHERE itemUrl = :itemUrl")
    suspend fun getNoteByUrl(itemUrl: String): NoteItem?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateNote(note: NoteItem)

    @Query("UPDATE notes SET is_deleted = 1, is_dirty = 1, updated_at = :timestamp WHERE itemUrl = :itemUrl")
    suspend fun softDeleteNote(itemUrl: String, timestamp: Long)

    @Query("UPDATE notes SET updated_at = :timestamp, is_dirty = 0 WHERE itemUrl = :itemUrl")
    suspend fun markNoteSynced(itemUrl: String, timestamp: Long)

    @Query("UPDATE notes SET is_dirty = 1")
    suspend fun markAllNotesDirty()

    @Query("UPDATE notes SET is_deleted = 0")
    suspend fun resetAllNotesIsDeleted()

    @Query("DELETE FROM notes WHERE is_deleted = 1")
    suspend fun deleteAllDeletedNotes()
}

@Database(
    entities = [NoteItem::class, NoteItemFts::class],
    version = 3,
    exportSchema = true,
)
@ConstructedBy(NotesDatabaseConstructor::class)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `notes` ADD COLUMN `supabase_id` TEXT DEFAULT ''")
                connection.execSQL("ALTER TABLE `notes` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `notes` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `notes` ADD COLUMN `is_deleted` INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("ALTER TABLE `notes` ADD COLUMN `is_dirty` INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Replaces the FTS4 index with FTS5 and refills it from existing notes; Room
        // re-creates the content sync triggers once migrations finish.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS `notes_fts`")
                connection.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS5(`content`, `itemTitle`, tokenize=`unicode61 remove_diacritics 2`, content=`notes`, prefix=`2 3`)")
                connection.execSQL("INSERT INTO notes_fts(notes_fts) VALUES('rebuild')")
            }
        }

        internal val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        fun getInstance(databaseBuilder: DatabaseBuilder): NotesDatabase =
            databaseBuilder
                .build<NotesDatabase>("notes.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
