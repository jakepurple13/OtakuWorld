package com.programmersbox.koogintegration.dynamictranslation.agent

import com.programmersbox.koogintegration.dynamictranslation.model.DynamicTranslationConfig
import com.programmersbox.koogintegration.dynamictranslation.strategy.AndroidOcrStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.AndroidRenderStrategy
import com.programmersbox.koogintegration.dynamictranslation.strategy.AndroidTranslationStrategy
import com.programmersbox.koogintegration.dynamictranslation.tool.TranslateTool

actual fun buildDynamicTranslationAgent(config: DynamicTranslationConfig): DynamicTranslationAgent {
    val ocr = AndroidOcrStrategy()
    val translation = AndroidTranslationStrategy()
    val render = AndroidRenderStrategy()
    val tool = TranslateTool(ocr, translation, render)
    return DynamicTranslationAgent(tool)
}
