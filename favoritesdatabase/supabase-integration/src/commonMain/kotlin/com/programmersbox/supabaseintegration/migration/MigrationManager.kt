package com.programmersbox.supabaseintegration.migration

import com.programmersbox.supabaseintegration.sync.SyncEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

enum class MigrationState { Unknown, Running, Complete, Failed }

interface MigrationPrefs {
    fun isMigrationComplete(): Boolean
    fun markMigrationComplete()
}

expect fun createMigrationPrefs(context: Any?): MigrationPrefs

class MigrationManager(
    private val syncEngine: SyncEngine,
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
            syncEngine.fullSync()
            prefs.markMigrationComplete()
            _migrationState.value = MigrationState.Complete
        }.onFailure {
            _migrationState.value = MigrationState.Failed
        }
    }
}
