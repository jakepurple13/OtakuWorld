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

class ExceptionDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: ExceptionDatabase
    private lateinit var dao: ExceptionDao

    private fun exception(time: Long, message: String = "message-$time") = ExceptionItem(
        time = time,
        message = message,
    )

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("exception-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ExceptionDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.exceptionDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `insertException then getAllExceptions returns it ordered by time descending`() = runTest {
        dao.insertException(exception(time = 1_000L))
        dao.insertException(exception(time = 2_000L))

        val all = dao.getAllExceptions().first()

        assertEquals(2, all.size)
        assertEquals(2_000L, all[0].time)
        assertEquals(1_000L, all[1].time)
    }

    @Test fun `getExceptionCount reflects inserts`() = runTest {
        assertEquals(0, dao.getExceptionCount().first())

        dao.insertException(exception(time = 1_000L))
        assertEquals(1, dao.getExceptionCount().first())

        dao.insertException(exception(time = 2_000L))
        assertEquals(2, dao.getExceptionCount().first())
    }

    @Test fun `insertException with Throwable persists a row with the stack trace as message`() = runTest {
        val throwable = RuntimeException("boom")

        dao.insertException(throwable)

        val all = dao.getAllExceptions().first()

        assertEquals(1, all.size)
        assertEquals(throwable.stackTraceToString(), all[0].message)
    }

    @Test fun `deleteException removes a row`() = runTest {
        val item = exception(time = 1_000L)
        dao.insertException(item)

        dao.deleteException(item)

        assertTrue(dao.getAllExceptions().first().isEmpty())
        assertNull(dao.getAllExceptions().first().firstOrNull())
    }

    @Test fun `deleteAll clears the table`() = runTest {
        dao.insertException(exception(time = 1_000L))
        dao.insertException(exception(time = 2_000L))

        dao.deleteAll()

        assertEquals(0, dao.getExceptionCount().first())
        assertTrue(dao.getAllExceptions().first().isEmpty())
    }
}
