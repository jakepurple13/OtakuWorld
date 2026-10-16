package com.programmersbox.kmpuiviews.domain

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.testing.FakeExceptionDao
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MediaUpdateCheckerTest {

    private lateinit var database: ItemDatabase

    private val favorite = DbModel(
        title = "Title",
        description = "Description",
        url = "https://example.com/1",
        imageUrl = "https://example.com/1.jpg",
        source = "ExampleService",
    )

    private fun checker(): MediaUpdateChecker {
        val sourceRepository = SourceRepository()
        return MediaUpdateChecker(
            dao = database.itemDao(),
            sourceRepository = sourceRepository,
            sourceLoader = SourceLoader(
                extensionsDir = { File.createTempFile("ext", "").also { it.delete(); it.mkdirs() } },
                sourceType = "test",
                sourceRepository = sourceRepository,
                appDirs = AppDirs {
                    appName = "MediaUpdateCheckerTest"
                    appAuthor = "jakepurple13"
                },
            ),
            exceptionDao = FakeExceptionDao(),
        )
    }

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `mapDbModel refreshes an existing saved notification so it sorts to the top`() = runTest {
        val dao = database.itemDao()
        dao.insertNotification(
            NotificationItem(
                id = 42,
                url = favorite.url,
                summaryText = "old summary",
                notiTitle = favorite.title,
                imageUrl = favorite.imageUrl,
                source = favorite.source,
                contentTitle = favorite.title,
                isShowing = false,
                supabaseId = "remote-id",
                createdAt = 1L,
            )
        )

        checker().mapDbModel(listOf(null to favorite)) { _, _, _ -> }

        val updated = dao.getNotificationItem(favorite.url)!!
        assertTrue(updated.createdAt > 1L, "createdAt should be bumped to now")
        assertNotEquals("old summary", updated.summaryText)
        assertTrue(updated.isShowing)
        assertEquals("remote-id", updated.supabaseId)
        assertEquals(1, dao.getAllNotifications().size)
    }
}
