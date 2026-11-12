package com.programmersbox.kmpuiviews.dailyoverview

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** A [SummaryEngine] backed by a Koog [LLMClient]. Each run opens a client and closes it when done. */
abstract class KoogSummaryEngine : SummaryEngine {
    /** The client and model to run, or null when the engine is not set up. */
    protected abstract suspend fun client(): Pair<LLMClient, LLModel>?

    /** False for clients without `executeStreaming` (LiteRT); those emit the whole reply once. */
    protected open val streams: Boolean = true

    final override fun summarize(facts: DailyFacts): Flow<String> =
        generate(SummaryPrompt.system(facts.unit), SummaryPrompt.user(facts))

    final override fun generate(system: String, user: String): Flow<String> = flow {
        val (client, model) = client() ?: error("$id is not set up")
        val prompt = prompt("daily-overview") {
            system(system)
            user(user)
        }
        client.use {
            if (!streams) {
                emit(client.execute(prompt, model).textContent())
                return@use
            }
            var sawDelta = false
            client.executeStreaming(prompt, model).collect { frame ->
                when (frame) {
                    is StreamFrame.TextDelta -> {
                        sawDelta = true
                        emit(frame.text)
                    }
                    // Only for clients that send the reply in one piece; otherwise it repeats the deltas.
                    is StreamFrame.TextComplete -> if (!sawDelta) emit(frame.text)
                    else -> Unit
                }
            }
        }
        // Building and closing clients blocks (LiteRT's close() is runBlocking), so keep it off Main.
    }.flowOn(Dispatchers.IO)
}
