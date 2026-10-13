package com.programmersbox.mihon.repo

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class MihonIndexV2GzipTest {
    @Test
    fun gzippedProtobufIndex() = runTest {
        val raw = ProtoBuf.encodeToByteArray(V2Index(name = "Store", signingKey = "indexKey", extensions = V2InlineList(V2_EXTENSIONS)))
        val gz = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(raw) } }.toByteArray()
        val indexUrl = "https://cdn.example/index.pb"
        val repo = MihonRepoClient(v2Client(mapOf("https://store.example/repo.json" to { repoJson(indexUrl) }, indexUrl to { gz })))
        assertV2Mapping(repo.fetch(listOf("https://store.example")))
        assertEquals(setOf("metaKey"), repo.signingKeys)
    }
}
