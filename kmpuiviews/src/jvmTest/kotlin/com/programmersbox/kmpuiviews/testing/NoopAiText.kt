package com.programmersbox.kmpuiviews.testing

import com.programmersbox.kmpuiviews.domain.aitext.AiEngineInfo
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** An [AiText] with no engine, for view model tests that never generate. */
class NoopAiText : AiText {
    override suspend fun engineFor(feature: AiFeature, inputLength: Int): AiEngineInfo? = null
    override fun recap(previousChapterText: String): Flow<String> = emptyFlow()
    override fun tldr(description: String): Flow<String> = emptyFlow()
    override fun proofread(text: String): Flow<String> = emptyFlow()
    override fun rewrite(text: String, style: RewriteStyle): Flow<String> = emptyFlow()
    override fun listNames(titles: List<String>): Flow<String> = emptyFlow()
    override fun listDescription(titles: List<String>): Flow<String> = emptyFlow()
}
