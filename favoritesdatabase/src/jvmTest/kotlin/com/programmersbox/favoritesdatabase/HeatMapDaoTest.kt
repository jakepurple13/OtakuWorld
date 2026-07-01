package com.programmersbox.favoritesdatabase

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeatMapDaoTest {

    private lateinit var dbFile: File
    private lateinit var database: HeatMapDatabase
    private lateinit var dao: HeatMapDao

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("heat-map-dao-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<HeatMapDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.heatMapDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        dbFile.delete()
    }

    @Test fun `insertHeatMap then getAllHeatMaps returns it`() = runTest {
        val date = LocalDate(2026, 1, 1)
        dao.insertHeatMap(HeatMapItem(time = date, count = 5))

        val all = dao.getAllHeatMaps().first()

        assertEquals(1, all.size)
        assertEquals(date, all[0].time)
        assertEquals(5, all[0].count)
    }

    @Test fun `getHeatMapByDate returns the matching row`() = runTest {
        val date = LocalDate(2026, 1, 1)
        dao.insertHeatMap(HeatMapItem(time = date, count = 3))

        val result = dao.getHeatMapByDate(date)

        assertEquals(date, result?.time)
        assertEquals(3, result?.count)
    }

    @Test fun `getHeatMapByDate returns null when no row exists`() = runTest {
        val result = dao.getHeatMapByDate(LocalDate(2026, 1, 1))

        assertNull(result)
    }

    @Test fun `upsertHeatMap on a fresh date creates count of 1`() = runTest {
        val date = LocalDate(2026, 1, 1)

        dao.upsertHeatMap(date)

        val result = dao.getHeatMapByDate(date)
        assertEquals(1, result?.count)
    }

    @Test fun `upsertHeatMap on an existing date increments count`() = runTest {
        val date = LocalDate(2026, 1, 1)

        dao.upsertHeatMap(date)
        dao.upsertHeatMap(date)

        val result = dao.getHeatMapByDate(date)
        assertEquals(2, result?.count)
    }

    @Test fun `deleteHeatMap removes the row`() = runTest {
        val date = LocalDate(2026, 1, 1)
        val item = HeatMapItem(time = date, count = 1)
        dao.insertHeatMap(item)

        dao.deleteHeatMap(item)

        assertNull(dao.getHeatMapByDate(date))
    }
}
