package com.programmersbox.supabaseintegration.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.SyncPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupPreferenceRepositoryTest {

    private lateinit var dbFile: File
    private lateinit var database: SyncPreferences
    private lateinit var repository: BackupPreferenceRepository

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("backup-preference-repo-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<SyncPreferences>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = BackupPreferenceRepository(database.backupPreferenceDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `isBackupEnabled defaults to true when no preference stored`() = runTest {
        assertTrue(repository.isBackupEnabled("notes"))
    }

    @Test
    fun `isBackupEnabled reflects a stored disabled preference`() = runTest {
        repository.setBackupEnabled("notes", false)

        assertEquals(false, repository.isBackupEnabled("notes"))
    }

    @Test
    fun `setBackupEnabled can flip a preference back on`() = runTest {
        repository.setBackupEnabled("notes", false)
        repository.setBackupEnabled("notes", true)

        assertTrue(repository.isBackupEnabled("notes"))
    }

    @Test
    fun `observeAllPreferences maps stored rows by tableName`() = runTest {
        repository.setBackupEnabled("notes", false)
        repository.setBackupEnabled("history", true)

        val prefs = repository.observeAllPreferences().first()

        assertEquals(mapOf("notes" to false, "history" to true), prefs)
    }
}
