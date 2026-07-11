package com.programmersbox.kmpuiviews.utils

import com.programmersbox.kmpuiviews.testing.FakeExceptionDao
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackupTest {

    @Test
    fun `createBackup rethrows and logs on zipper failure`() = runBlocking {
        // Uses a real Zipper with no registered processors and a bogus path to force failure.
        val exceptionDao = FakeExceptionDao()
        val zipper = Zipper(backupProcessors = emptyList())
        val backup = Backup(exceptionDao, zipper)
        val badFile = PlatformFile("/nonexistent/path/backup.zip")

        assertFailsWith<Exception> {
            backup.createBackup(badFile, setOf("a.json")) { }
        }
        assertEquals(1, exceptionDao.insertedExceptions.size)
    }
}
