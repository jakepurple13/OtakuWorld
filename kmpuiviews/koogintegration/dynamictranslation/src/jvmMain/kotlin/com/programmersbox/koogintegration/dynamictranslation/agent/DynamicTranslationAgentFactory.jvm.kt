package com.programmersbox.koogintegration.dynamictranslation.agent

import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModels
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.strategy.JvmOcrStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.JvmRenderStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.JvmTranslationStrategy
import com.programmersbox.koogintegration.dynamictranslation.tool.TranslateTool
import kotlinx.coroutines.runBlocking
import java.io.File

actual fun buildDynamicTranslationAgent(config: DynamicTranslationConfig): DynamicTranslationAgent {
    val client = OllamaClient()
    val model = OllamaModels.models.firstOrNull { it.id == config.ollamaModel }
        ?: OllamaModels.Meta.LLAMA_3_2

    val ocr = JvmOcrStrategy()
    val translation = JvmTranslationStrategy(client, model)
    val render = JvmRenderStrategy()
    val tool = TranslateTool(ocr, translation, render)

    return DynamicTranslationAgent(tool)
}


fun main() = runBlocking {
    val config = DynamicTranslationConfig(
        ollamaModel = "llama3.2",
        tessDataPath = "/Users/jacobrein/Downloads/tessdata",
        sourceLanguage = "eng",
        targetLanguage = "eng",
    )
    val d = buildDynamicTranslationAgent(config).translate(
        File("/Users/jacobrein/Downloads/ruri.jpeg").readBytes(),
        config
    )

    println(d)

    val f = File("/Users/jacobrein/Downloads/ruri1.jpeg")
    f.createNewFile()
    f.writeBytes(d.imageBytes)
}