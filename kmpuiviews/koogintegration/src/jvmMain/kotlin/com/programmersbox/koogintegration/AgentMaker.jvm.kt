package com.programmersbox.koogintegration

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel


actual fun platformModels(modelCompany: String): List<LLModel> = emptyList()
actual fun getModelLinkToDownload(name: String): String? = null
actual class PlatformAgents {
    actual fun platformLLMProviders(): List<LLMProvider> = listOf()
    actual fun isPlatformProvider(modelCompany: String, apiKey: String?): LLMClient? = null
    actual fun isPlatformModel(modelCompany: String, modelName: String): LLModel? = null
}

actual fun canDownloadModel(name: String): Boolean = false