package com.programmersbox.cloudstream.plugins

import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.extractorApis
import com.programmersbox.cloudstream.FakeMainApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginRegistrationsTest {

    private class FakeExtractor : ExtractorApi() {
        override val name = "FakeExtractor"
        override val mainUrl = "https://extract.test"
        override val requiresReferer = false
    }

    @Test
    fun capture_returnsOnlyWhatTheBlockRegistered() {
        val existing = FakeMainApi("Existing")
        APIHolder.allProviders.add(existing)
        val api = FakeMainApi("New")
        val extractor = FakeExtractor()
        try {
            val (apis, extractors) = PluginRegistrations.capture {
                APIHolder.allProviders.add(api)
                extractorApis.add(extractor)
            }

            assertEquals(listOf(api), apis)
            assertEquals(listOf<ExtractorApi>(extractor), extractors)
        } finally {
            APIHolder.allProviders.remove(existing)
            PluginRegistrations.remove(listOf(api), listOf(extractor))
        }
    }

    @Test
    fun capture_rollsBackPartialRegistrationsWhenTheBlockThrows() {
        val api = FakeMainApi("Partial")
        val extractor = FakeExtractor()

        assertFailsWith<IllegalStateException> {
            PluginRegistrations.capture {
                APIHolder.allProviders.add(api)
                APIHolder.addPluginMapping(api)
                extractorApis.add(extractor)
                error("load failed")
            }
        }

        assertFalse(APIHolder.allProviders.any { it === api })
        assertFalse(APIHolder.apis.any { it === api })
        assertFalse(extractorApis.any { it === extractor })
    }

    @Test
    fun remove_dropsProvidersMappingsAndExtractors() {
        val api = FakeMainApi("Removed")
        val extractor = FakeExtractor()
        val (apis, extractors) = PluginRegistrations.capture {
            APIHolder.allProviders.add(api)
            APIHolder.addPluginMapping(api)
            extractorApis.add(extractor)
        }
        assertTrue(APIHolder.allProviders.any { it === api })

        PluginRegistrations.remove(apis, extractors)

        assertFalse(APIHolder.allProviders.any { it === api })
        assertFalse(APIHolder.apis.any { it === api })
        assertFalse(extractorApis.any { it === extractor })
    }
}
