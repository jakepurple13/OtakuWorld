package com.programmersbox.extensioninterfaces

interface Extension {
    val manifest: ExtensionManifest

    suspend fun getPopular(page: Int): List<ExtensionItem>
    suspend fun getLatest(page: Int): List<ExtensionItem>
    suspend fun search(query: String, page: Int): List<ExtensionItem>
    suspend fun getDetail(url: String): ExtensionDetail
    suspend fun getContent(url: String): ExtensionContent
}
