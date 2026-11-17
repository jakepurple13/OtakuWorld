package com.programmersbox.kmpuiviews.dailyoverview

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.LocalModel
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryPrompt
import com.programmersbox.kmpuiviews.domain.dailyoverview.liteRtDisplayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_label_on_device
import otakuworld.kmpuiviews.generated.resources.do_setup_import_model
import java.io.File

/**
 * Desktop on-device engine: the newest `.litertlm` file in [modelsDir], run with LiteRT-LM on the CPU.
 * Each run loads the model and closes it after, like the Android Gemma engine. Reading the status only
 * lists the folder, so the native library is never loaded until a model runs.
 */
class LocalModelEngine(
    private val modelsDir: File,
    private val cacheDir: File,
) : SummaryEngine, LocalModel {
    override val id = SummaryEngineId.Gemma
    override val onDevice = true

    override suspend fun currentFile(): File? = model()

    /** The model this engine runs, or null when the folder has none. */
    fun model(): File? = modelsDir.listFiles()
        .orEmpty()
        .filter { it.isFile && it.name.endsWith(".litertlm", ignoreCase = true) }
        .maxByOrNull { it.lastModified() }

    override suspend fun label(): String = getString(Res.string.do_label_on_device, shortName())

    override suspend fun shortName(): String = model()?.let { liteRtDisplayName(it.name) } ?: "Local model"

    override suspend fun status(): EngineStatus =
        if (model() != null) EngineStatus.Ready
        else EngineStatus.NeedsSetup(getString(Res.string.do_setup_import_model))

    override fun summarize(facts: DailyFacts): Flow<String> =
        generate(SummaryPrompt.system(facts.unit), SummaryPrompt.user(facts))

    override fun generate(system: String, user: String): Flow<String> = flow {
        val file = model() ?: error("No local model")
        emitAll(run(file, system, user))
    }

    override suspend fun verify(file: File) {
        run(file, "Reply with one word.", "Say OK.").collect()
    }

    private fun run(file: File, system: String, user: String): Flow<String> = flow {
        val engine = Engine(
            EngineConfig(
                modelPath = file.absolutePath,
                backend = Backend.CPU(),
                // Without the cache folder every load repacks the weights in memory again
                cacheDir = cacheDir.apply { mkdirs() }.absolutePath,
            )
        )
        // close() throws on an engine whose initialize() failed, so only close a loaded one
        engine.initialize()
        try {
            engine.createConversation(ConversationConfig(systemInstruction = Contents.of(system))).use { conversation ->
                conversation.sendMessageAsync(user).collect { emit(it.toString()) }
            }
        } finally {
            engine.close()
        }
    }.flowOn(Dispatchers.IO)
}
