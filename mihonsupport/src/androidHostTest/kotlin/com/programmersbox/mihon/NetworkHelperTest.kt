package com.programmersbox.mihon

import android.content.ContextWrapper
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.brotli.BrotliInterceptor
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

class NetworkHelperTest {

    private val context = object : ContextWrapper(null) {
        private val cache = createTempDirectory("mihon-cache").toFile()
        override fun getCacheDir(): File = cache
    }

    // Keiyoushi's KeiSource throws "BrotliInterceptor must not be present in default client".
    @Test
    fun defaultClientHasNoBrotliInterceptor() {
        val client = NetworkHelper(context).client

        assertTrue(client.interceptors.none { it is BrotliInterceptor })
        assertTrue(client.networkInterceptors.none { it is BrotliInterceptor })
    }
}
