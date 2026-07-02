package com.programmersbox.kmpuiviews.repository

import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.kmpuiviews.testing.createTestItemDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The JVM [NotificationRepository] actual is a no-op stub: none of its methods touch
 * [ItemDao][com.programmersbox.favoritesdatabase.ItemDao] or any real notification system
 * (there is no desktop notification implementation yet). These tests simply verify each
 * method completes without throwing.
 */
class NotificationRepositoryTest {

    private lateinit var database: ItemDatabase

    private fun notificationItem(url: String, id: Int = 1) = NotificationItem(
        id = id,
        url = url,
        summaryText = "Summary",
        notiTitle = "Title",
        imageUrl = "https://example.com/$url.jpg",
        source = "ExampleService",
        contentTitle = "Content",
    )

    @BeforeTest
    fun setUp() {
        database = createTestItemDatabase()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun repository() = NotificationRepository(itemDao = database.itemDao())

    @Test fun `cancelById completes without throwing`() = runTest {
        repository().cancelById(1)
    }

    @Test fun `cancelNotification completes without throwing`() = runTest {
        repository().cancelNotification(notificationItem("https://example.com/1"))
    }

    @Test fun `cancelGroup completes without throwing`() = runTest {
        repository().cancelGroup()
    }
}
