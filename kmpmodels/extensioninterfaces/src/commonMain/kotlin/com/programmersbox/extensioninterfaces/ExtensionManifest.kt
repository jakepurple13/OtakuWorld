package com.programmersbox.extensioninterfaces

data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String?,
    val description: String?,
    val iconUrl: String?,
    val updateUrl: String?,
)
