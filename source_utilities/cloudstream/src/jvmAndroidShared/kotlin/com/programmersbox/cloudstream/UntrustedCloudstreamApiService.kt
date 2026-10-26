package com.programmersbox.cloudstream

import com.programmersbox.kmpmodels.KmpUntrustedApiService

class UntrustedCloudstreamApiService(
    val internalName: String,
    override val reason: String,
    override val canTrust: Boolean,
    private val onTrust: suspend () -> Unit,
) : KmpUntrustedApiService {
    override val baseUrl: String = ""
    override val serviceName: String = "cloudstream-untrusted:$internalName"
    override val displayName: String = internalName
    override val notWorking: Boolean = true
    override suspend fun trust() = onTrust()
}
