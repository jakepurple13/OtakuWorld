package com.programmersbox.extensioninterfaces

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionItem(
    val title: String,
    val url: String,
    val imageUrl: String?,
)

@Serializable
data class ExtensionChapter(
    val name: String,
    val url: String,
    val uploaded: String?,
)

@Serializable
data class ExtensionDetail(
    val title: String,
    val url: String,
    val imageUrl: String?,
    val description: String?,
    val genres: List<String>,
    val chapters: List<ExtensionChapter>,
)

@Serializable
data class ExtensionContent(
    val urls: List<String>,
    val headers: Map<String, String> = emptyMap(),
)
