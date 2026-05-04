package com.programmersbox.kmpextensionloader

import android.app.Application
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.models.ApiService
import com.programmersbox.models.SourceInformation
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmModelMapperTest {

    private val mockApp = Application("com.test", File(System.getProperty("java.io.tmpdir")))
    private val mapper = JvmModelMapper(mockApp)

    private val stubService = object : ApiService {
        override val baseUrl = "https://example.com"
        override val serviceName = "TestService"
    }

    @Test fun `mapApiService preserves baseUrl`() {
        val kmp: KmpApiService = mapper.mapApiService(stubService)
        assertEquals("https://example.com", kmp.baseUrl)
    }

    @Test fun `mapApiService preserves serviceName`() {
        val kmp: KmpApiService = mapper.mapApiService(stubService)
        assertEquals("TestService", kmp.serviceName)
    }

    @Test fun `mapApiService preserves canScroll default false`() {
        assertEquals(false, mapper.mapApiService(stubService).canScroll)
    }

    @Test fun `mapSourceInformation preserves name and packageName`() {
        val si = SourceInformation(
            apiService = stubService,
            name = "My Source",
            icon = null,
            packageName = "com.example.pkg",
        )
        val kmp = mapper.mapSourceInformation(si)
        assertEquals("My Source", kmp.name)
        assertEquals("com.example.pkg", kmp.packageName)
    }

    @Test fun `mapSourceInformation icon is always null`() {
        val si = SourceInformation(
            apiService = stubService, name = "x", icon = null, packageName = "pkg"
        )
        assertNull(mapper.mapSourceInformation(si).icon)
    }
}
