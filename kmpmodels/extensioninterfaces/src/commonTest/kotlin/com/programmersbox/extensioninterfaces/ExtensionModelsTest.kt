package com.programmersbox.extensioninterfaces

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun extensionItem_roundTripsThroughJson() {
        val item = ExtensionItem(title = "Chapter 1", url = "https://example.com/1", imageUrl = null)
        val encoded = json.encodeToString(ExtensionItem.serializer(), item)
        val decoded = json.decodeFromString(ExtensionItem.serializer(), encoded)
        assertEquals(item, decoded)
    }

    @Test
    fun extensionDetail_roundTripsThroughJson() {
        val detail = ExtensionDetail(
            title = "My Manga",
            url = "https://example.com/manga",
            imageUrl = "https://example.com/manga.png",
            description = "A manga",
            genres = listOf("Action", "Adventure"),
            chapters = listOf(ExtensionChapter(name = "Ch. 1", url = "https://example.com/1", uploaded = "2026-01-01")),
        )
        val encoded = json.encodeToString(ExtensionDetail.serializer(), detail)
        val decoded = json.decodeFromString(ExtensionDetail.serializer(), encoded)
        assertEquals(detail, decoded)
    }

    @Test
    fun extensionUpdateInfo_roundTripsThroughJson() {
        val update = ExtensionUpdateInfo(
            id = "my-extension",
            latestVersion = "1.2.0",
            downloadUrl = "https://example.com/my-extension.js",
            changelog = "Fixed a bug",
        )
        val encoded = json.encodeToString(ExtensionUpdateInfo.serializer(), update)
        val decoded = json.decodeFromString(ExtensionUpdateInfo.serializer(), encoded)
        assertEquals(update, decoded)
    }
}
