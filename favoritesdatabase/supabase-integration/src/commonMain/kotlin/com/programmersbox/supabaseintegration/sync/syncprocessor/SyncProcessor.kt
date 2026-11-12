package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

abstract class SyncProcessor<LocalModel, RemoteModel : Any>(
    val tableName: String,
) {

    // ==========================================
    // Abstract Methods (Implemented per table)
    // ==========================================

    // Metadata
    abstract val displayName: String
    abstract val backupPreferenceRepository: BackupPreferenceRepository

    // Push Requirements
    abstract suspend fun getDirtyItems(): List<LocalModel>
    abstract fun observeDirtyItems(): Flow<Int>
    abstract fun isLocalDeleted(local: LocalModel): Boolean
    abstract fun getLocalUpdatedAt(local: LocalModel): Long
    abstract fun toRemoteRow(local: LocalModel, uid: String, app: String, timestamp: Long): RemoteModel
    // Must clear is_dirty only while the row's updated_at still equals getLocalUpdatedAt(local),
    // so an edit made during the push stays dirty and is pushed next time.
    abstract suspend fun markLocalSynced(local: LocalModel, timestamp: Long)
    abstract suspend fun deleteLocal(local: LocalModel)

    // Runs local writes for one push chunk or pull page in a single database transaction.
    open suspend fun runInTransaction(block: suspend () -> Unit) = block()

    // Flags every local row for re-push (used to tag pre-existing remote rows with this app)
    abstract suspend fun markAllDirty()

    // The concrete class knows its type, so it handles the actual Supabase upsert call
    abstract suspend fun performUpsert(client: SupabaseClient, items: List<RemoteModel>)

    // Pull Requirements
    abstract fun isRemoteDeleted(remote: RemoteModel): Boolean
    abstract fun getRemoteUpdatedAt(remote: RemoteModel): Long
    abstract suspend fun getLocalEquivalent(remote: RemoteModel): LocalModel?
    abstract suspend fun upsertLocal(remote: RemoteModel)

    // The concrete class handles the Supabase select & decodeList call
    abstract suspend fun performSelect(postgrestResult: PostgrestResult): List<RemoteModel>

    // ==========================================
    // Backup Preference
    // ==========================================

    suspend fun isBackupEnabled(): Boolean = backupPreferenceRepository.isBackupEnabled(tableName)

    // ==========================================
    // Shared Sync Logic
    // ==========================================

    open suspend fun push(client: SupabaseClient, uid: String, app: String) {
        if (!isBackupEnabled()) return

        val dirty = getDirtyItems()
        if (dirty.isEmpty()) return

        println("Pushing ${dirty.size} items to $tableName")
        val errors = mutableListOf<Throwable>()

        dirty.chunked(500).forEach { chunk ->
            runCatching {
                // Same timestamp for the remote row and the local stamp
                val timestamps = chunk.map { model ->
                    val updatedAt = getLocalUpdatedAt(model)
                    if (updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else updatedAt
                }
                val rowsToUpsert = chunk.mapIndexed { index, model -> toRemoteRow(model, uid, app, timestamps[index]) }

                // Delegate to subclass to bypass the reified type error
                performUpsert(client, rowsToUpsert)

                runInTransaction {
                    chunk.forEachIndexed { index, model ->
                        markLocalSynced(model, timestamps[index])
                        if (isLocalDeleted(model)) {
                            deleteLocal(model)
                        }
                    }
                }
            }.onFailure { errors.add(it) }
        }

        if (errors.isNotEmpty()) throw errors.first()
    }

    open suspend fun pull(client: SupabaseClient, uid: String, app: String, since: Long) {
        if (!isBackupEnabled()) return

        // Each page is written in one transaction as it arrives
        fetchAllRecords(client, uid, app, since) { page ->
            println("Pulling ${page.size} items from $tableName")
            runInTransaction {
                page.forEach { row ->
                    val local = getLocalEquivalent(row)
                    val localUpdatedAt = local?.let { getLocalUpdatedAt(it) } ?: -1L

                    if (local == null || getRemoteUpdatedAt(row) > localUpdatedAt) {
                        if (isRemoteDeleted(row)) {
                            if (local != null) deleteLocal(local)
                        } else {
                            upsertLocal(row)
                        }
                    }
                }
            }
        }
    }

    private suspend inline fun fetchAllRecords(
        client: SupabaseClient,
        uid: String,
        app: String,
        since: Long,
        onPage: (List<RemoteModel>) -> Unit,
    ) {
        val pageSize = 1000L
        var offset = 0L

        while (true) {
            val toIndex = offset + pageSize - 1

            // Delegate to subclass to bypass the reified type error
            val batch = performSelect(
                client.postgrest[tableName].select {
                    range(offset, toIndex)
                    filter {
                        eq("user_id", uid)
                        eq("app", app)
                        gt("updated_at", since)
                    }
                }
            )

            if (batch.isNotEmpty()) onPage(batch)
            if (batch.size < pageSize) break
            offset += pageSize
        }
    }
}
