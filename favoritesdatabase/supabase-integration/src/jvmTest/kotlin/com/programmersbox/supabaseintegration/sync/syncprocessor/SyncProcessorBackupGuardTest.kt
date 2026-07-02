package com.programmersbox.supabaseintegration.sync.syncprocessor

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.SyncPreferences
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class TestSyncProcessor(
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<String, String>(tableName = "test_table") {
    override val displayName: String = "Test Table"

    var getDirtyItemsCallCount = 0
    var performSelectCallCount = 0

    override suspend fun getDirtyItems(): List<String> {
        getDirtyItemsCallCount++
        return emptyList()
    }

    override fun observeDirtyItems(): Flow<Int> = flowOf(0)
    override fun isLocalDeleted(local: String) = false
    override fun getLocalUpdatedAt(local: String) = 0L
    override fun toRemoteRow(local: String, uid: String, timestamp: Long) = local
    override suspend fun markLocalSynced(local: String, timestamp: Long) {}
    override suspend fun deleteLocal(local: String) {}
    override suspend fun performUpsert(client: SupabaseClient, items: List<String>) {}
    override fun isRemoteDeleted(remote: String) = false
    override fun getRemoteUpdatedAt(remote: String) = 0L
    override suspend fun getLocalEquivalent(remote: String): String? = null
    override suspend fun upsertLocal(remote: String) {}

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<String> {
        performSelectCallCount++
        return emptyList()
    }
}

class SyncProcessorBackupGuardTest {

    private lateinit var dbFile: File
    private lateinit var database: SyncPreferences
    private lateinit var repository: BackupPreferenceRepository
    private lateinit var processor: TestSyncProcessor

    private val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://example.supabase.co",
        supabaseKey = "test-key",
    ) { install(Postgrest) }

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("sync-processor-guard-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<SyncPreferences>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = BackupPreferenceRepository(database.backupPreferenceDao())
        processor = TestSyncProcessor(repository)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test
    fun `push skips work when backup disabled for this table`() = runTest {
        repository.setBackupEnabled("test_table", false)

        processor.push(client, uid = "user-1")

        assertEquals(0, processor.getDirtyItemsCallCount)
    }

    @Test
    fun `push runs when backup enabled for this table`() = runTest {
        repository.setBackupEnabled("test_table", true)

        processor.push(client, uid = "user-1")

        assertEquals(1, processor.getDirtyItemsCallCount)
    }

    @Test
    fun `push runs by default when no preference is stored`() = runTest {
        processor.push(client, uid = "user-1")

        assertEquals(1, processor.getDirtyItemsCallCount)
    }

    @Test
    fun `pull skips work when backup disabled for this table`() = runTest {
        repository.setBackupEnabled("test_table", false)

        processor.pull(client, uid = "user-1", since = 0L)

        assertEquals(0, processor.performSelectCallCount)
    }
}
