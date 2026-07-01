package com.programmersbox.kmpmodels

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExampleServiceTest {

    private val service = ExampleService()

    @Test fun `baseUrl is the example domain`() {
        assertEquals("https://example.com/", service.baseUrl)
    }

    @Test fun `recent returns a single example item pointing back at the service`() = runTest {
        val items = service.recent(page = 1)
        assertEquals(1, items.size)
        assertEquals("Example", items[0].title)
        assertEquals(service, items[0].source)
    }

    @Test fun `itemInfo returns 10 chapters in reverse order`() = runTest {
        val item = service.recent(1)[0]
        val info = service.itemInfo(item)
        assertEquals(10, info.chapters.size)
        assertEquals("Example 9", info.chapters.first().name)
        assertEquals("Example 0", info.chapters.last().name)
    }

    @Test fun `chapterInfo returns 3 pages of storage links`() = runTest {
        val chapter = service.itemInfo(service.recent(1)[0]).chapters.first()
        val storage = service.chapterInfo(chapter)
        assertEquals(3, storage.size)
        assertEquals(listOf("Page 1", "Page 2", "Page 3"), storage.map { it.filename })
        assertEquals(chapter.url, storage[0].source)
    }

    @Test fun `getSourceInformation exposes the example package name`() {
        val info = ExampleService.getSourceInformation()
        assertEquals("com.example", info.packageName)
        assertEquals("Example", info.name)
    }
}
