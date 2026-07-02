package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupPreferenceDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: SyncPreferences
    private lateinit var dao: BackupPreferenceDao

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("sync-preferences-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<SyncPreferences>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.backupPreferenceDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `getPreference returns null when no row exists`() = runTest {
        assertNull(dao.getPreference("notes"))
    }

    @Test
    fun `upsertPreference then getPreference returns stored value`() = runTest {
        dao.upsertPreference(BackupPreferenceEntity(tableName = "notes", enabled = false))

        val result = dao.getPreference("notes")

        assertEquals("notes", result?.tableName)
        assertEquals(false, result?.enabled)
    }

    @Test
    fun `upsertPreference replaces existing row for the same tableName`() = runTest {
        dao.upsertPreference(BackupPreferenceEntity(tableName = "notes", enabled = false))
        dao.upsertPreference(BackupPreferenceEntity(tableName = "notes", enabled = true))

        assertEquals(true, dao.getPreference("notes")?.enabled)
    }

    @Test
    fun `observeAllPreferences emits every stored row`() = runTest {
        dao.upsertPreference(BackupPreferenceEntity(tableName = "notes", enabled = false))
        dao.upsertPreference(BackupPreferenceEntity(tableName = "history", enabled = true))

        val all = dao.observeAllPreferences().first()

        assertEquals(2, all.size)
        assertTrue(all.any { it.tableName == "notes" && !it.enabled })
        assertTrue(all.any { it.tableName == "history" && it.enabled })
    }
}
