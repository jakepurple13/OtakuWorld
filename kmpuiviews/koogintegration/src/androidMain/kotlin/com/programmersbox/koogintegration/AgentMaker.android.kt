package com.programmersbox.koogintegration

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.litert.LiteRTClientConfig
import ai.koog.prompt.executor.clients.litert.LiteRTLLMClient
import ai.koog.prompt.executor.clients.litert.LiteRTLLMProvider
import ai.koog.prompt.executor.clients.litert.LiteRTLLModels
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import android.content.Context
import java.io.File


actual fun platformModels(modelCompany: String): List<LLModel> = when (modelCompany) {
    LiteRTLLMProvider.display -> LiteRTLLModels.models
    else -> emptyList()
}

actual fun getModelLinkToDownload(name: String): String? {
    return when (name) {
        LiteRTLLModels.Gemma4E2B.id -> "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        LiteRTLLModels.Gemma4E4B.id -> "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
        else -> null
    }
}

actual class PlatformAgents(
    private val context: Context,
) {
    actual fun platformLLMProviders(): List<LLMProvider> = listOf(
        LiteRTLLMProvider
    )

    actual fun isPlatformProvider(
        modelCompany: String,
        apiKey: String?,
    ): LLMClient? = when (modelCompany) {
        LiteRTLLMProvider.display -> LiteRTLLMClient(
            config = LiteRTClientConfig(
                modelsPath = File(context.filesDir, "models").absolutePath,
                cacheDir = File(context.cacheDir, "models").absolutePath
            )
        )

        else -> null
    }

    actual fun isPlatformModel(modelCompany: String, modelName: String): LLModel? = when (modelCompany) {
        LiteRTLLMProvider.display -> LiteRTLLModels.models.find { it.id == modelName }
        else -> null
    }
}

actual fun canDownloadModel(name: String): Boolean = when (name) {
    LiteRTLLMProvider.display -> true
    else -> false
}