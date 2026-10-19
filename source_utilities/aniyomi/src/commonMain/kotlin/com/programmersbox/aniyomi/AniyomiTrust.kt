package com.programmersbox.aniyomi

/** Mirrors Aniyomi's TrustExtension: repo-signed, or explicitly trusted by the user for this exact version. */
object AniyomiTrust {
    fun trustId(pkg: String, versionCode: Long, fingerprint: String): String = "$pkg:$versionCode:$fingerprint"

    fun isTrusted(
        pkg: String,
        versionCode: Long,
        fingerprints: Set<String>,
        repoSigningKeys: Set<String>,
        trustedIds: Set<String>,
    ): Boolean {
        if (fingerprints.isEmpty()) return false
        return fingerprints.any { it in repoSigningKeys } ||
            fingerprints.any { trustId(pkg, versionCode, it) in trustedIds }
    }
}
