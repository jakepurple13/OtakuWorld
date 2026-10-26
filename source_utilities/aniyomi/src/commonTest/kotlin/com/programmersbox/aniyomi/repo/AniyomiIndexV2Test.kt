@file:OptIn(ExperimentalSerializationApi::class)

package com.programmersbox.aniyomi.repo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.test.Test
import kotlin.test.assertEquals

// Field numbers of upstream Aniyomi's NetworkAnimeExtensionStore (what real repos such as Yūzōnō publish).
@Serializable
private data class UpstreamStore(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(3) val signingKey: String,
    @ProtoNumber(101) val extensionList: UpstreamList,
)

@Serializable
private data class UpstreamList(@ProtoNumber(1) val extensions: List<UpstreamExtension>)

@Serializable
private data class UpstreamExtension(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val packageName: String,
    @ProtoNumber(6) val versionName: String,
    @ProtoNumber(8) val isTorrent: Boolean,
    @ProtoNumber(9) val sources: List<UpstreamSource>,
)

@Serializable
private data class UpstreamSource(
    @ProtoNumber(1) val id: Long,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val language: String,
    @ProtoNumber(6) val message: String,
)

class AniyomiIndexV2Test {
    @Test
    fun decodesUpstreamFieldNumbers() {
        val bytes = ProtoBuf.encodeToByteArray(
            UpstreamStore(
                name = "Repo",
                signingKey = "key",
                extensionList = UpstreamList(
                    listOf(
                        UpstreamExtension(
                            name = "Torrent Ext",
                            packageName = "eu.kanade.tachiyomi.animeextension.en.torrent",
                            versionName = "14.3",
                            isTorrent = true,
                            sources = listOf(
                                UpstreamSource(1, "A", "en", "note"),
                                UpstreamSource(2, "B", "ja", ""),
                            ),
                        )
                    )
                ),
            )
        )

        val extension = (ProtoBuf.decodeFromByteArray<V2Index>(bytes).extensions as V2InlineList)
            .extensionList.extensions.single()

        assertEquals(true, extension.isTorrent)
        assertEquals(listOf("en", "ja"), extension.sources.map { it.language })
        assertEquals("note", extension.sources.first().message)
    }
}
