package com.programmersbox.kmpuiviews.updates

import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.updater.Version
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GhRelease(
    @SerialName("tag_name") val tagName: String,
    val prerelease: Boolean,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GhAsset>,
)

@Serializable
data class GhAsset(val name: String)

private val stableTag = Regex("""^v\d+\.\d+\.\d+$""")

fun metadataFileName(channel: String, platform: Platform): String = when (platform) {
    Platform.MacOS -> "$channel-mac.yml"
    Platform.Linux -> "$channel-linux.yml"
    Platform.Windows, Platform.Unknown -> "$channel.yml"
}

private fun GhRelease.hasAsset(name: String) = assets.any { it.name == name }

/** [releases] are newest first; returns the first non-prerelease `vX.Y.Z` release carrying [ymlName]. */
fun selectStable(releases: List<GhRelease>, ymlName: String): GhRelease? =
    releases.firstOrNull { !it.prerelease && stableTag.matches(it.tagName) && it.hasAsset(ymlName) }

/** [releases] are newest first; returns the first `nightly_` prerelease carrying [ymlName]. */
fun selectNightly(releases: List<GhRelease>, ymlName: String): GhRelease? =
    releases.firstOrNull { it.prerelease && it.tagName.startsWith("nightly_") && it.hasAsset(ymlName) }

/** Each pair is a release plus its yml `version`; returns the release with the higher version. */
fun pickHigher(a: Pair<GhRelease, String>?, b: Pair<GhRelease, String>?): GhRelease? = when {
    a == null -> b?.first
    b == null -> a.first
    Version.fromString(a.second) >= Version.fromString(b.second) -> a.first
    else -> b.first
}
