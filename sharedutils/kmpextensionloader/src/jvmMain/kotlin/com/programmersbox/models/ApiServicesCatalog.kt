package com.programmersbox.models

import android.app.Application
import android.content.pm.PackageInfo

interface ApiServicesCatalog {
    val name: String
    fun createSources(): List<ApiService>
}

interface ExternalApiServicesCatalog : ApiServicesCatalog {
    val hasRemoteSources: Boolean
    suspend fun initialize(app: Application)
    fun getSources(): List<SourceInformation>
    override fun createSources(): List<ApiService> = getSources().map { it.apiService }
    suspend fun getRemoteSources(): List<RemoteSources> = emptyList()
    fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean = false
}

interface ExternalCustomApiServicesCatalog : ApiServicesCatalog {
    val hasRemoteSources: Boolean
    suspend fun initialize(app: Application)
    fun getSources(): List<SourceInformation>
    override fun createSources(): List<ApiService> = getSources().map { it.apiService }
    fun shouldReload(packageName: String, packageInfo: PackageInfo): Boolean = false
    suspend fun getRemoteSources(customUrls: List<String>): List<RemoteSources> = emptyList()
}
