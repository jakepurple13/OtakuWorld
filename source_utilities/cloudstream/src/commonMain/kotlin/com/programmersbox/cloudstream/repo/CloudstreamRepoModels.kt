package com.programmersbox.cloudstream.repo

import kotlinx.serialization.Serializable

/** A CloudStream `repo.json`: a name plus URLs of one or more `plugins.json` lists. */
@Serializable
data class CsRepo(
    val name: String,
    val manifestVersion: Int,
    val pluginLists: List<String>,
    val description: String? = null,
    val iconUrl: String? = null,
)

/** One entry of a CloudStream `plugins.json`. [status] 0 means the plugin is disabled upstream. */
@Serializable
data class CsPlugin(
    val url: String,
    val status: Int,
    val version: Int,
    val apiVersion: Int,
    val name: String,
    val internalName: String,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val repositoryUrl: String? = null,
    val tvTypes: List<String>? = null,
    val language: String? = null,
    val iconUrl: String? = null,
    val fileSize: Long? = null,
    val fileHash: String? = null,
)
