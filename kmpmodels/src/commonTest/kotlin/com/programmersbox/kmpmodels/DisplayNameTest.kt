package com.programmersbox.kmpmodels

import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayNameTest {
    private class PlainService : KmpApiService {
        override val baseUrl: String = ""
    }

    private class NamedService : KmpApiService {
        override val baseUrl: String = ""
        override val serviceName: String = "key:1"
        override val displayName: String = "Pretty"
    }

    @Test
    fun displayNameDefaultsToServiceName() {
        val service = PlainService()
        assertEquals("PlainService", service.serviceName)
        assertEquals(service.serviceName, service.displayName)
    }

    @Test
    fun displayNameCanBeOverriddenWithoutChangingServiceName() {
        val service = NamedService()
        assertEquals("key:1", service.serviceName)
        assertEquals("Pretty", service.displayName)
    }
}
