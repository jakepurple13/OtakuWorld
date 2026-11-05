package com.programmersbox.anime.shared.subtitlegen

import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSubtitleGeneratorTest {
    private val root = createTempDirectory("subgen-test").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val testSettings = TestDesktopSettings(root, scope)
    private val settings = testSettings.settings
    private val modelsDir = File(root, "models").apply {
        mkdirs()
        File(this, "ggml-small.bin").writeBytes("lmgg".encodeToByteArray() + ByteArray(16))
    }
    // Nothing is downloaded: SMALL (the default) is already on disk.
    private val models = WhisperModelStore(settings, modelsDir, OkHttpClient(), scope, "http://127.0.0.1:1/")
    private val runner = FakeRunner()
    private val sidecarsChanged = AtomicInteger()
    private val finished = CopyOnWriteArrayList<String>()

    private val ja = whisperLanguages.first { it.code == "ja" }
    private val en = whisperLanguages.first { it.code == "en" }
    private val jaToEn = GenerationRequest(ja, en)

    private fun generator(whisper: suspend () -> File? = { File("/fake/whisper-cli") }) = DesktopSubtitleGenerator(
        scope = scope,
        settings = settings,
        models = models,
        ffmpeg = { File("/fake/ffmpeg") },
        whisper = whisper,
        runner = runner,
        onSidecarsChanged = { sidecarsChanged.incrementAndGet() },
        trayState = null,
    ).also { gen ->
        scope.launch(start = CoroutineStart.UNDISPATCHED) { gen.finished.collect { finished += it } }
    }

    private fun video(name: String): File =
        File(root, "videos").apply { mkdirs() }.let { File(it, "$name.mkv") }.apply { writeText("v") }

    @AfterTest
    fun cleanup() {
        scope.cancel()
        testSettings.close()
        root.deleteRecursively()
    }

    private fun waitFor(block: () -> Boolean) = runBlocking {
        withTimeout(10_000) { while (!block()) delay(20) }
    }

    /**
     * Records every command. ffmpeg calls succeed at once; whisper calls (the ones with `-ovtt`) wait on the next
     * queued gate if any, print progress, then fail with the next queued failure line or write `<outBase>.vtt`.
     */
    private class FakeRunner : CommandRunner {
        val calls = CopyOnWriteArrayList<List<String>>()
        val gates = ConcurrentLinkedQueue<CompletableDeferred<Unit>>()
        val failures = ConcurrentLinkedQueue<String>()
        val whisperStarted = AtomicInteger()

        override suspend fun run(args: List<String>, onLine: (String) -> Unit): Int {
            calls += args
            if ("-ovtt" !in args) return 0
            whisperStarted.incrementAndGet()
            gates.poll()?.await()
            onLine("progress = 50%")
            onLine("progress = 100%")
            failures.poll()?.let { line ->
                onLine(line)
                onLine("")
                return 1
            }
            File(args[args.indexOf("-of") + 1] + ".vtt").writeText("WEBVTT\n")
            return 0
        }

        /** The temp dir of the job that extracted [video]'s audio. */
        fun tempDirOf(video: File): File {
            val extract = calls.first { it.contains(video.path) }
            return File(extract.last()).parentFile
        }
    }

    @Test
    fun runsAndSavesSidecar() {
        val gen = generator()
        val a = video("Frieren - Episode 07")
        gen.enqueue(a.absolutePath, jaToEn, remember = false)

        waitFor { finished.isNotEmpty() }
        assertEquals(listOf(a.absolutePath), finished)
        assertEquals(emptyMap(), gen.statuses.value)
        assertEquals(listOf("English (generated)"), OfflineSubtitles.find(a).map { it.lang })
        assertEquals(1, sidecarsChanged.get())
        assertFalse(runner.tempDirOf(a).exists())
        val whisper = runner.calls.single { "-ovtt" in it }
        assertTrue("-tr" in whisper)
        assertEquals(File(modelsDir, "ggml-small.bin").path, whisper[whisper.indexOf("-m") + 1])
    }

    @Test
    fun secondJobQueuesBehindFirst() {
        val gen = generator()
        val gate = CompletableDeferred<Unit>().also { runner.gates += it }
        val a = video("Frieren - Episode 07")
        val b = video("Frieren - Episode 08")
        gen.enqueue(a.absolutePath, jaToEn, remember = false)
        waitFor { runner.whisperStarted.get() == 1 }
        gen.enqueue(b.absolutePath, jaToEn, remember = false)

        assertEquals(GenerationStatus.Queued(afterTitle = "Episode 07"), gen.statuses.value[b.absolutePath])
        assertTrue(gen.statuses.value[a.absolutePath] is GenerationStatus.Transcribing)

        gate.complete(Unit)
        waitFor { finished.size == 2 }
        assertEquals(listOf(a.absolutePath, b.absolutePath), finished)
        assertEquals(emptyMap(), gen.statuses.value)
    }

    @Test
    fun cancelActiveCleansUpAndStartsNext() {
        val gen = generator()
        runner.gates += CompletableDeferred()
        val a = video("Frieren - Episode 07")
        val b = video("Frieren - Episode 08")
        gen.enqueue(a.absolutePath, jaToEn, remember = false)
        gen.enqueue(b.absolutePath, jaToEn, remember = false)
        waitFor { runner.whisperStarted.get() == 1 }

        gen.cancel(a.absolutePath)
        assertEquals(null, gen.statuses.value[a.absolutePath])

        waitFor { finished.isNotEmpty() }
        assertEquals(listOf(b.absolutePath), finished)
        assertEquals(emptyList(), OfflineSubtitles.find(a))
        assertFalse(runner.tempDirOf(a).exists())
        assertEquals(listOf("English (generated)"), OfflineSubtitles.find(b).map { it.lang })
        assertEquals(emptyMap(), gen.statuses.value)
    }

    @Test
    fun cancelQueuedRemovesIt() {
        val gen = generator()
        val gate = CompletableDeferred<Unit>().also { runner.gates += it }
        val a = video("Frieren - Episode 07")
        val b = video("Frieren - Episode 08")
        gen.enqueue(a.absolutePath, jaToEn, remember = false)
        waitFor { runner.whisperStarted.get() == 1 }
        gen.enqueue(b.absolutePath, jaToEn, remember = false)

        gen.cancel(b.absolutePath)
        assertEquals(null, gen.statuses.value[b.absolutePath])
        gate.complete(Unit)

        waitFor { finished.isNotEmpty() }
        assertEquals(listOf(a.absolutePath), finished)
        assertTrue(runner.calls.none { b.path in it })
    }

    @Test
    fun failureKeepsStatusAndMovesOn() {
        val gen = generator()
        runner.failures += "error: failed to open model"
        val a = video("Frieren - Episode 07")
        val b = video("Frieren - Episode 08")
        gen.enqueue(a.absolutePath, jaToEn, remember = false)
        gen.enqueue(b.absolutePath, jaToEn, remember = false)

        waitFor { finished.isNotEmpty() }
        assertEquals(listOf(b.absolutePath), finished)
        assertEquals(
            mapOf(a.absolutePath to GenerationStatus.Failed("error: failed to open model", jaToEn)),
            gen.statuses.value,
        )
        assertEquals(emptyList(), OfflineSubtitles.find(a))
        assertFalse(runner.tempDirOf(a).exists())

        gen.dismiss(a.absolutePath)
        assertEquals(emptyMap(), gen.statuses.value)
    }

    @Test
    fun missingCustomModelFails() = runBlocking {
        val custom = File(root, "ggml-mine.bin").apply { writeBytes("lmgg".encodeToByteArray() + ByteArray(16)) }
        models.addCustom(custom)
        custom.delete()
        val gen = generator()
        val a = video("Frieren - Episode 07")
        gen.enqueue(a.absolutePath, jaToEn, remember = false)

        waitFor { gen.statuses.value[a.absolutePath] is GenerationStatus.Failed }
        assertEquals(GenerationStatus.Failed("Model file not found", jaToEn), gen.statuses.value[a.absolutePath])
    }

    @Test
    fun missingWhisperFails() {
        val gen = generator(whisper = { null })
        val a = video("Frieren - Episode 07")
        gen.enqueue(a.absolutePath, jaToEn, remember = false)

        waitFor { gen.statuses.value[a.absolutePath] is GenerationStatus.Failed }
        assertEquals(
            GenerationStatus.Failed(
                "whisper-cli not found. Install whisper.cpp or set its location in Settings › Subtitles.",
                jaToEn,
            ),
            gen.statuses.value[a.absolutePath],
        )
    }

    @Test
    fun sameAsSpokenSavesOriginalTrack() {
        val gen = generator()
        val a = video("Frieren - Episode 07")
        gen.enqueue(a.absolutePath, GenerationRequest(WhisperAuto, WhisperLanguage("auto", "Same as spoken")), remember = false)

        waitFor { finished.isNotEmpty() }
        assertEquals(listOf("Original (generated)"), OfflineSubtitles.find(a).map { it.lang })
        assertFalse("-tr" in runner.calls.single { "-ovtt" in it })
    }

    @Test
    fun rememberSavesLanguages() {
        val gen = generator()
        assertEquals(jaToEn, gen.lastRequest.value)
        val ko = whisperLanguages.first { it.code == "ko" }
        gen.enqueue(video("Frieren - Episode 07").absolutePath, GenerationRequest(ko, en), remember = true)

        waitFor { gen.lastRequest.value == GenerationRequest(ko, en) }
    }

    @Test
    fun processRunnerMergesStderrAndStopsOnCancel() = runBlocking {
        if (System.getProperty("os.name").startsWith("Windows")) return@runBlocking
        val lines = CopyOnWriteArrayList<String>()
        val exit = ProcessCommandRunner.run(listOf("/bin/sh", "-c", "echo out; echo 'progress = 50%' 1>&2; exit 3")) { lines += it }
        assertEquals(3, exit)
        assertEquals(listOf("out", "progress = 50%"), lines)

        val started = System.currentTimeMillis()
        val job = launch(Dispatchers.IO) { ProcessCommandRunner.run(listOf("/bin/sh", "-c", "echo started; exec sleep 30")) { lines += it } }
        waitFor { "started" in lines }
        job.cancel()
        withTimeout(5_000) { job.join() }
        assertTrue(System.currentTimeMillis() - started < 10_000)
    }
}
