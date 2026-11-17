package com.programmersbox.kmpuiviews.dailyoverview

import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalModelEngineTest {
    private val root = Files.createTempDirectory("local-model").toFile()
    private val models = File(root, "models").apply { mkdirs() }
    private val engine = LocalModelEngine(models, File(root, "cache"))

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `no model means NeedsSetup and model is null`() = runBlocking<Unit> {
        File(models, "notes.txt").writeText("x")
        assertEquals(SummaryEngineId.Gemma, engine.id)
        assertNull(engine.model())
        assertIs<EngineStatus.NeedsSetup>(engine.status())
    }

    @Test
    fun `newest litertlm wins`() = runBlocking<Unit> {
        File(models, "old.litertlm").apply { writeText("x"); setLastModified(1_000) }
        File(models, "gemma3-1b-it-int4.litertlm").apply { writeText("x"); setLastModified(2_000) }
        assertEquals("gemma3-1b-it-int4.litertlm", engine.model()?.name)
        assertEquals(EngineStatus.Ready, engine.status())
    }

    @Test
    fun `short name is the model display name`() = runBlocking<Unit> {
        File(models, "gemma3-1b-it-int4.litertlm").writeText("x")
        assertEquals("Gemma3 1b It Int4", engine.shortName())
    }

    // Loads a real model when LITERTLM_MODEL points at a .litertlm file; skipped otherwise.
    @Test
    fun `real model generates`() = runBlocking<Unit> {
        val path = System.getenv("LITERTLM_MODEL") ?: return@runBlocking
        val file = File(path)
        engine.verify(file)
        Files.copy(file.toPath(), File(models, file.name).toPath())
        val reply = engine.generate("Answer in one short sentence.", "What color is the sky?").toList().joinToString("")
        println("LocalModelEngine reply: $reply")
        assertTrue(reply.isNotBlank())
    }
}
