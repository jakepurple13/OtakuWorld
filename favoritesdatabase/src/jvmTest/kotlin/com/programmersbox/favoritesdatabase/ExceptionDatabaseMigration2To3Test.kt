package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExceptionDatabaseMigration2To3Test {

    private val dbFile = File.createTempFile("exception-migration-2-3-test", ".db").also {
        it.delete()
        it.deleteOnExit()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    @Test fun `rows survive the primary key change and same-millisecond inserts no longer collide`() = runTest {
        createDatabaseAtVersion(
            file = dbFile,
            databaseClass = "com.programmersbox.favoritesdatabase.ExceptionDatabase",
            version = 2,
            seed = listOf(
                "INSERT INTO ExceptionTable (time, message) VALUES (1000, 'old one')",
                "INSERT INTO ExceptionTable (time, message) VALUES (2000, 'old two')",
            ),
        )

        val database = Room.databaseBuilder<ExceptionDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(*ExceptionDatabase.MIGRATIONS)
            .build()
        val dao = database.exceptionDao()

        assertEquals(listOf("old two", "old one"), dao.getAllExceptions().first().map { it.message })

        dao.insertException(ExceptionItem(time = 3000, message = "a"))
        dao.insertException(ExceptionItem(time = 3000, message = "b"))
        assertEquals(4, dao.getExceptionCount().first())

        database.close()
    }
}
