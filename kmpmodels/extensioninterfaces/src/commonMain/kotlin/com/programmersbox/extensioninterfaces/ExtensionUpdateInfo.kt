package com.programmersbox.extensioninterfaces

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionUpdateInfo(
    val id: String,
    val latestVersion: String,
    val downloadUrl: String,
    val changelog: String? = null,
)
