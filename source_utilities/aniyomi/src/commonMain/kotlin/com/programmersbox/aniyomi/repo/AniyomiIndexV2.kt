@file:OptIn(ExperimentalSerializationApi::class)

package com.programmersbox.aniyomi.repo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoOneOf

// Aniyomi "index_v2" store index. Field numbers follow upstream NetworkAnimeExtensionStore, which differs
// from Mihon's: Extension has isTorrent = 8 and sources = 9, Source has message = 6.

@Serializable
data class V2Index(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val badgeLabel: String = "",
    @ProtoNumber(3) val signingKey: String = "",
    @ProtoNumber(4) val contact: V2Contact? = null,
    /** proto `oneof extensions { ExtensionList extensionList = 101; string extensionListUrl = 102; }` */
    @ProtoOneOf val extensions: V2Extensions? = null,
)

@Serializable
sealed interface V2Extensions

@Serializable
data class V2InlineList(@ProtoNumber(101) val extensionList: V2ExtensionList) : V2Extensions

@Serializable
data class V2ListUrl(@ProtoNumber(102) val extensionListUrl: String) : V2Extensions

@Serializable
data class V2Contact(
    @ProtoNumber(1) val website: String = "",
    @ProtoNumber(2) val discord: String? = null,
)

@Serializable
data class V2ExtensionList(@ProtoNumber(1) val extensions: List<V2Extension> = emptyList())

@Serializable
data class V2Extension(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val packageName: String = "",
    @ProtoNumber(3) val resources: V2Resources? = null,
    @ProtoNumber(4) val extensionLib: String = "",
    @ProtoNumber(5) val versionCode: Long = 0,
    @ProtoNumber(6) val versionName: String = "",
    @ProtoNumber(7) val contentWarning: V2ContentWarning = V2ContentWarning.CONTENT_WARNING_UNSPECIFIED,
    @ProtoNumber(8) val isTorrent: Boolean = false,
    @ProtoNumber(9) val sources: List<V2Source> = emptyList(),
)

@Serializable
data class V2Resources(
    @ProtoNumber(1) val apkUrl: String = "",
    @ProtoNumber(2) val iconUrl: String = "",
)

@Serializable
data class V2Source(
    @ProtoNumber(1) val id: Long = 0,
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val language: String = "",
    @ProtoNumber(4) val homeUrl: String = "",
    @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
    @ProtoNumber(6) val message: String? = null,
)

@Serializable
enum class V2ContentWarning {
    @ProtoNumber(0) CONTENT_WARNING_UNSPECIFIED,
    @ProtoNumber(1) CONTENT_WARNING_SAFE,
    @ProtoNumber(2) CONTENT_WARNING_MIXED,
    @ProtoNumber(3) CONTENT_WARNING_NSFW,
}

/**
 * proto3-JSON shape of [V2Index]: the oneof appears as two optional keys instead of a nested
 * polymorphic object. Nested messages reuse the protobuf classes (JSON keys = field names).
 */
@Serializable
internal data class V2IndexJson(
    val name: String = "",
    val signingKey: String = "",
    val extensionList: V2ExtensionList? = null,
    val extensionListUrl: String? = null,
) {
    fun toIndex() = V2Index(
        name = name,
        signingKey = signingKey,
        extensions = extensionList?.let(::V2InlineList) ?: extensionListUrl?.let(::V2ListUrl),
    )
}
