package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IncognitoRepositoryTest {

    private lateinit var database: ItemDatabase

    private fun repository() = IncognitoRepository(
        dao = database.itemDao(),
        systemAlerter = SystemAlerter(),
    )

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test fun `addIncognito inserts a row`() = runTest {
        repository().addIncognito(url = "https://example.com/1", title = "Example")

        val result = database.itemDao().getIncognitoSourceSync("https://example.com/1")

        assertTrue(result != null && result.isIncognito)
        assertEquals("Example", result?.name)
    }

    @Test fun `removeIncognito deletes the row`() = runTest {
        val repo = repository()
        repo.addIncognito(url = "https://example.com/1", title = "Example")

        repo.removeIncognito(url = "https://example.com/1")

        assertNull(database.itemDao().getIncognitoSourceSync("https://example.com/1"))
    }

    @Test fun `updateIncognito flips the isIncognito flag`() = runTest {
        val repo = repository()
        repo.addIncognito(url = "https://example.com/1", title = "Example", isIncognito = true)

        repo.updateIncognito(url = "https://example.com/1", isIncognito = false)

        val result = database.itemDao().getIncognitoSourceSync("https://example.com/1")
        assertTrue(result != null && !result.isIncognito)
    }
}
