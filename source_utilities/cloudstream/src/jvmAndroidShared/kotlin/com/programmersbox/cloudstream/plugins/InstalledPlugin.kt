package com.programmersbox.cloudstream.plugins

import kotlinx.serialization.Serializable

@Serializable
data class InstalledPlugin(
    val repoUrl: String,
    val internalName: String,
    val version: Int,
    val path: String,
)
