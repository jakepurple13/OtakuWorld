package com.programmersbox.jsextensionloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExtensionManifestParserTest {

    @Test
    fun parsesHeaderCommentMetadata() {
        val script = """
            // name: My Extension
            // version: 1.0.0
            // author: Jane Doe
            // description: A test extension
            // iconUrl: https://example.com/icon.png
            // updateUrl: https://example.com/update.json
            function getPopular(page) { return []; }
        """.trimIndent()

        val manifest = ExtensionManifestParser.parse(script, companionManifestJson = null, sourceId = "my-extension")

        assertEquals("my-extension", manifest.id)
        assertEquals("My Extension", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals("Jane Doe", manifest.author)
        assertEquals("A test extension", manifest.description)
        assertEquals("https://example.com/icon.png", manifest.iconUrl)
        assertEquals("https://example.com/update.json", manifest.updateUrl)
    }

    @Test
    fun headerCommentStopsAtFirstNonCommentLine() {
        val script = """
            // name: My Extension
            // version: 1.0.0
            function getPopular(page) { return []; }
            // author: this should be ignored, it's after code
        """.trimIndent()

        val manifest = ExtensionManifestParser.parse(script, companionManifestJson = null, sourceId = "my-extension")

        assertEquals("My Extension", manifest.name)
        assertNull(manifest.author)
    }

    @Test
    fun companionManifestJsonTakesPrecedenceOverHeaderComment() {
        val script = """
            // name: Header Name
            // version: 0.0.1
            function getPopular(page) { return []; }
        """.trimIndent()
        val companionJson = """
            {"name": "JSON Name", "version": "2.0.0", "sourceType": "manga"}
        """.trimIndent()

        val manifest = ExtensionManifestParser.parse(script, companionManifestJson = companionJson, sourceId = "my-extension")

        assertEquals("JSON Name", manifest.name)
        assertEquals("2.0.0", manifest.version)
        assertEquals("my-extension", manifest.id)
    }

    @Test
    fun companionManifestJsonExplicitIdOverridesSourceId() {
        val companionJson = """
            {"id": "explicit-id", "name": "JSON Name", "version": "2.0.0"}
        """.trimIndent()

        val manifest = ExtensionManifestParser.parse("", companionManifestJson = companionJson, sourceId = "my-extension")

        assertEquals("explicit-id", manifest.id)
    }
}
