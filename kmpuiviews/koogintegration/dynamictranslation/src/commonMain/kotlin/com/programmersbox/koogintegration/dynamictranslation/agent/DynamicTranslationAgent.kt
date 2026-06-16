package com.programmersbox.koogintegration.dynamictranslation.agent

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationOutput
import com.programmersbox.koogintegration.dynamictranslation.tool.TranslateTool
import java.io.Closeable

class DynamicTranslationAgent(
    private val tool: TranslateTool,
) : Closeable {

    suspend fun translate(
        imageBytes: ByteArray,
        config: DynamicTranslationConfig,
    ): DynamicTranslationOutput = tool.execute(imageBytes, config)

    override fun close() {
        tool.close()
    }
}
