package com.programmersbox.mihon

import com.programmersbox.kmpmodels.KmpUntrustedApiService

class UntrustedMihonApiService(
    val packageName: String,
    override val reason: String,
    override val canTrust: Boolean,
    label: String,
    private val onTrust: suspend () -> Unit,
) : KmpUntrustedApiService {
    override val baseUrl: String = ""
    override val serviceName: String = "mihon-untrusted:$packageName"
    override val displayName: String = label
    override val notWorking: Boolean = true
    override suspend fun trust() = onTrust()
}
