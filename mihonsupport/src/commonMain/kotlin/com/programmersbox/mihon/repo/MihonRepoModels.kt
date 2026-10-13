package com.programmersbox.mihon.repo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MihonRepo(
    val meta: Meta,
    /** URL of the Mihon index_v2 (TachiyomiX) index; when present it replaces `index.min.json`. */
    @SerialName("index_v2") val indexV2: String? = null,
) {
    @Serializable
    data class Meta(
        val name: String,
        val shortName: String? = null,
        val website: String = "",
        /** Required for legacy repos; index_v2 repos may instead carry it in the index's `signingKey`. */
        val signingKeyFingerprint: String? = null,
    )
}

@Serializable
data class MihonIndexEntry(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String = "",
    val code: Long = 0,
    val version: String,
    val nsfw: Int = 0,
    val sources: List<Source> = emptyList(),
) {
    @Serializable
    data class Source(val name: String, val lang: String = "", val baseUrl: String = "")
}
