package com.programmersbox.kmpmodels

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelsSerializationTest {

    @Test fun `KmpStorage round-trips through JSON with all fields set`() {
        val original = KmpStorage(
            sub = "English",
            source = "https://example.com/chapter/1",
            link = "https://example.com/page1.jpg",
            quality = "1080p",
            filename = "Page 1",
        )

        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<KmpStorage>(json)

        assertEquals(original, decoded)
    }

    @Test fun `KmpStorage round-trips through JSON with all fields null`() {
        val original = KmpStorage()

        val decoded = Json.decodeFromString<KmpStorage>(Json.encodeToString(original))

        assertEquals(original, decoded)
        assertNull(decoded.sub)
        assertNull(decoded.link)
    }
}
