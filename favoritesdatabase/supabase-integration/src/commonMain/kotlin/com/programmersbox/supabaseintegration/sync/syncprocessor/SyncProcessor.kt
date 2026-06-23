package com.programmersbox.supabaseintegration.sync.syncprocessor

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

    // Push Requirements
    abstract suspend fun getDirtyItems(): List<LocalModel>
    abstract fun observeDirtyItems(): Flow<Int>
    abstract fun isLocalDeleted(local: LocalModel): Boolean
    abstract fun getLocalUpdatedAt(local: LocalModel): Long
    abstract fun toRemoteRow(local: LocalModel, uid: String, timestamp: Long): RemoteModel
    abstract suspend fun markLocalSynced(local: LocalModel, timestamp: Long)
    abstract suspend fun deleteLocal(local: LocalModel)

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
    // Shared Sync Logic
    // ==========================================

    open suspend fun push(client: SupabaseClient, uid: String) {
        val dirty = getDirtyItems()
        if (dirty.isEmpty()) return

        println("Pushing ${dirty.size} items to $tableName")
        val errors = mutableListOf<Throwable>()

        dirty.chunked(500).forEach { chunk ->
            runCatching {
                val rowsToUpsert = chunk.map { model ->
                    val updatedAt = getLocalUpdatedAt(model)
                    val timestamp = if (updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else updatedAt
                    toRemoteRow(model, uid, timestamp)
                }

                // Delegate to subclass to bypass the reified type error
                performUpsert(client, rowsToUpsert)

                chunk.forEach { model ->
                    val updatedAt = getLocalUpdatedAt(model)
                    val timestamp = if (updatedAt == 0L) Clock.System.now().toEpochMilliseconds() else updatedAt

                    markLocalSynced(model, timestamp)
                    if (isLocalDeleted(model)) {
                        deleteLocal(model)
                    }
                }
            }.onFailure { errors.add(it) }
        }

        if (errors.isNotEmpty()) throw errors.first()
    }

    open suspend fun pull(client: SupabaseClient, uid: String, since: Long) {
        val allRecords = fetchAllRecords(client, uid, since)
        if (allRecords.isEmpty()) return

        println("Pulling ${allRecords.size} items from $tableName")

        allRecords.forEach { row ->
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

    private suspend inline fun fetchAllRecords(client: SupabaseClient, uid: String, since: Long): List<RemoteModel> {
        val allRecords = mutableListOf<RemoteModel>()
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
                        gt("updated_at", since)
                    }
                }
            )

            allRecords.addAll(batch)
            if (batch.size < pageSize) break
            offset += pageSize
        }
        return allRecords
    }
}