package com.programmersbox.aniyomi

import com.programmersbox.kmpmodels.KmpUntrustedApiService

class UntrustedAniyomiApiService(
    val packageName: String,
    private val versionCode: Long,
    private val fingerprint: String,
    override val reason: String,
    override val canTrust: Boolean,
    private val onTrust: suspend (String) -> Unit,
) : KmpUntrustedApiService {
    override val baseUrl: String = ""
    override val serviceName: String = "aniyomi-untrusted:$packageName"
    override val displayName: String = packageName
    override val notWorking: Boolean = true
    override suspend fun trust() = onTrust(AniyomiTrust.trustId(packageName, versionCode, fingerprint))
}
