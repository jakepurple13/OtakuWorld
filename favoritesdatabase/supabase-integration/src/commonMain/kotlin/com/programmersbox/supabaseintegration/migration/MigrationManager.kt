package com.programmersbox.supabaseintegration.migration

import com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

enum class MigrationState { Unknown, Running, Complete, Failed }

interface MigrationPrefs {
    fun isMigrationComplete(): Boolean
    fun markMigrationComplete()
}

/**
 * Rows pushed before the `app` column existed have `app = NULL` and are invisible to pulls.
 * Marking every local row dirty once makes the next push re-upload them tagged with this app.
 */
class MigrationManager(
    private val syncProcessors: List<SyncProcessor<*, *>>,
    private val prefs: MigrationPrefs,
) {
    private val _migrationState = MutableStateFlow(MigrationState.Unknown)
    val migrationState: Flow<MigrationState> = _migrationState

    suspend fun runIfNeeded() {
        if (prefs.isMigrationComplete()) {
            _migrationState.value = MigrationState.Complete
            return
        }
        _migrationState.value = MigrationState.Running
        runCatching {
            syncProcessors.forEach { it.markAllDirty() }
            prefs.markMigrationComplete()
            _migrationState.value = MigrationState.Complete
        }.onFailure {
            _migrationState.value = MigrationState.Failed
        }
    }
}
