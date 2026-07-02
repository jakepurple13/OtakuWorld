package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.BookmarkedChapterRow
import com.programmersbox.supabaseintegration.sync.toBookmarkedChapter
import com.programmersbox.supabaseintegration.sync.toBookmarkedChapterRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow

class BookmarksSyncProcessor(
    private val bookmarkDao: BookmarkDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<BookmarkedChapter, BookmarkedChapterRow>(
    tableName = "bookmarked_chapters"
) {
    override val displayName: String = "Bookmarks"

    // ==========================================
    // Push Implementations
    // ==========================================

    override suspend fun getDirtyItems(): List<BookmarkedChapter> =
        bookmarkDao.getDirtyBookmarks()

    override fun observeDirtyItems(): Flow<Int> = bookmarkDao.observeDirtyBookmarkCount()

    override fun isLocalDeleted(local: BookmarkedChapter): Boolean =
        local.isDeleted

    override fun getLocalUpdatedAt(local: BookmarkedChapter): Long =
        local.updatedAt

    override fun toRemoteRow(local: BookmarkedChapter, uid: String, timestamp: Long): BookmarkedChapterRow =
        local.toBookmarkedChapterRow(uid, timestamp)

    override suspend fun markLocalSynced(local: BookmarkedChapter, timestamp: Long) {
        bookmarkDao.markBookmarkSynced(local.chapterUrl, timestamp)
    }

    override suspend fun deleteLocal(local: BookmarkedChapter) {
        bookmarkDao.deleteBookmark(local)
    }

    override suspend fun performUpsert(client: SupabaseClient, items: List<BookmarkedChapterRow>) {
        client.postgrest[tableName].upsert(items) {
            onConflict = "user_id,chapter_url"
        }
    }

    // ==========================================
    // Pull Implementations
    // ==========================================

    override fun isRemoteDeleted(remote: BookmarkedChapterRow): Boolean =
        remote.isDeleted

    override fun getRemoteUpdatedAt(remote: BookmarkedChapterRow): Long =
        remote.updatedAt

    override suspend fun getLocalEquivalent(remote: BookmarkedChapterRow): BookmarkedChapter? =
        bookmarkDao.getBookmarkByChapterUrl(remote.chapterUrl)

    override suspend fun upsertLocal(remote: BookmarkedChapterRow) {
        bookmarkDao.insertBookmark(remote.toBookmarkedChapter())
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<BookmarkedChapterRow> {
        return postgrestResult.decodeList<BookmarkedChapterRow>()
    }
}