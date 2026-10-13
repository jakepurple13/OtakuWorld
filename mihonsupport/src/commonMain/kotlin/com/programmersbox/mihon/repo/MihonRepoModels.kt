package com.programmersbox.mihon.repo

import kotlinx.serialization.Serializable

@Serializable
data class MihonRepo(val meta: Meta) {
    @Serializable
    data class Meta(
        val name: String,
        val shortName: String? = null,
        val website: String = "",
        val signingKeyFingerprint: String,
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
