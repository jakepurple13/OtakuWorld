package com.programmersbox.koogintegration

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.litert.LiteRTClientConfig
import ai.koog.prompt.executor.clients.litert.LiteRTLLMClient
import ai.koog.prompt.executor.clients.litert.LiteRTLLMProvider
import ai.koog.prompt.executor.clients.litert.LiteRTLLModels
import ai.koog.prompt.llm.LLMCapability
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
    private val modelManager: ModelManager,
) {
    actual fun platformLLMProviders(): List<LLMProvider> = listOf(
        LiteRTLLMProvider
    )

    actual fun isPlatformProvider(
        modelCompany: String,
        apiKey: String?,
    ): LLMClient? = when (modelCompany) {
        LiteRTLLMProvider.display -> liteRtClient()
        else -> null
    }

    /**
     * LiteRT models whose file is in the AI Helper's models folder. A LiteRT model id is its file name.
     * A `.litertlm` file Koog doesn't know (e.g. a smaller model copied in by hand) is registered as a
     * plain text model, so any LiteRT-LM model placed in the folder can be used. Newest first, so an
     * imported model is the one used when AI Helper hasn't picked one.
     */
    fun downloadedLiteRtModels(): List<LLModel> {
        val files = modelManager.listModels()
            .sortedByDescending { it.lastModifiedEpochMillis }
            .map { it.fileName }
            .filter { it.endsWith(".litertlm") }
        return files.map { file -> LiteRTLLModels.models.find { it.id == file } ?: registerCustomModel(file) }
    }

    private fun registerCustomModel(file: String): LLModel = synchronized(LiteRTLLModels) {
        LiteRTLLModels.models.find { it.id == file } ?: textModel(file).also { LiteRTLLModels.addCustomModel(it) }
    }

    /** A plain LiteRT text model for [file], without adding it to Koog's model list. */
    fun textModel(file: String): LLModel = LiteRTLLModels.models.find { it.id == file } ?: LLModel(
        provider = LiteRTLLMProvider,
        id = file,
        capabilities = listOf(LLMCapability.Completion),
        // Small models are often built with a short context; our prompts are well under this
        contextLength = 4_096,
        maxOutputTokens = 1_024,
    )

    /** The folder LiteRT models live in. */
    val liteRtModelsDir: File get() = File(context.filesDir, "models")

    /** A client for the models folder, or for [modelsPath] (e.g. an import being checked). */
    fun liteRtClient(modelsPath: String = liteRtModelsDir.absolutePath): LLMClient = LiteRTLLMClient(
        config = LiteRTClientConfig(
            modelsPath = modelsPath,
            // LiteRT can't create this folder itself. Without it the XNNPack weight cache can't be
            // written, so every load keeps the repacked weights in RAM (~2.2 GB even for a 1B model)
            // and low-memory phones kill the app.
            cacheDir = File(context.cacheDir, "models").apply { mkdirs() }.absolutePath
        )
    )

    actual fun isPlatformModel(modelCompany: String, modelName: String): LLModel? = when (modelCompany) {
        LiteRTLLMProvider.display -> LiteRTLLModels.models.find { it.id == modelName }
        else -> null
    }
}

actual fun canDownloadModel(name: String): Boolean = when (name) {
    LiteRTLLMProvider.display -> true
    else -> false
}