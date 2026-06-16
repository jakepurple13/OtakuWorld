package com.programmersbox.koogintegration.customscraper

import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpChapterModel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomScrapeKmpChapterModelTest {

    @Test
    fun serializesToJson() {
        val model = CustomScrapeKmpChapterModel(
            urls = listOf("https://example.com/page1.jpg", "https://example.com/page2.jpg")
        )
        val json = Json.encodeToString(CustomScrapeKmpChapterModel.serializer(), model)
        assertEquals("""{"urls":["https://example.com/page1.jpg","https://example.com/page2.jpg"]}""", json)
    }

    @Test
    fun deserializesFromJson() {
        val json = """{"urls":["https://example.com/video.mp4"]}"""
        val model = Json.decodeFromString(CustomScrapeKmpChapterModel.serializer(), json)
        assertEquals(listOf("https://example.com/video.mp4"), model.urls)
    }

    @Test
    fun emptyUrlsRoundTrips() {
        val model = CustomScrapeKmpChapterModel(urls = emptyList())
        val json = Json.encodeToString(CustomScrapeKmpChapterModel.serializer(), model)
        val decoded = Json.decodeFromString(CustomScrapeKmpChapterModel.serializer(), json)
        assertEquals(model, decoded)
    }
}
