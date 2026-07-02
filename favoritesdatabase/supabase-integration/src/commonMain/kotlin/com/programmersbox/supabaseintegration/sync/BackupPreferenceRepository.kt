package com.programmersbox.supabaseintegration.sync

import com.programmersbox.favoritesdatabase.BackupPreferenceDao
import com.programmersbox.favoritesdatabase.BackupPreferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BackupPreferenceRepository(
    private val backupPreferenceDao: BackupPreferenceDao,
) {
    suspend fun isBackupEnabled(tableName: String): Boolean =
        backupPreferenceDao.getPreference(tableName)?.enabled ?: true

    suspend fun setBackupEnabled(tableName: String, enabled: Boolean) {
        backupPreferenceDao.upsertPreference(BackupPreferenceEntity(tableName = tableName, enabled = enabled))
    }

    fun observeAllPreferences(): Flow<Map<String, Boolean>> =
        backupPreferenceDao.observeAllPreferences().map { preferences ->
            preferences.associate { it.tableName to it.enabled }
        }
}
