package com.programmersbox.kmpuiviews.domain.dailyoverview

/** Gemini Nano feature state, mirroring ML Kit's FeatureStatus without the Android dependency. */
enum class NanoFeatureStatus { Available, Downloadable, Downloading, Unavailable }

fun NanoFeatureStatus.toEngineStatus(
    downloadingReason: String = "Downloading Gemini Nano",
): EngineStatus = when (this) {
    NanoFeatureStatus.Available -> EngineStatus.Ready
    NanoFeatureStatus.Downloadable, NanoFeatureStatus.Downloading -> EngineStatus.NeedsSetup(downloadingReason)
    NanoFeatureStatus.Unavailable -> EngineStatus.Unavailable
}
