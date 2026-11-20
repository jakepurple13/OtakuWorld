package com.programmersbox.kmpuiviews.domain.aitext.modelimport

import kotlinx.coroutines.CancellationException
import com.programmersbox.telemetry.events.AiModelInstalled
import com.programmersbox.telemetry.NoOpAnalytics
import com.programmersbox.telemetry.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Copies a picked `.litertlm` file into [modelsDir] and checks that it loads, one import at a time.
 *
 * The copy and the check happen in [STAGING_DIR] inside [modelsDir]. The model list only reads regular
 * files at the top of the folder, so a half-copied or unchecked model never shows up as an engine. The
 * file moves into place only after [verify] passes; a model it replaces is deleted only then.
 *
 * App-scoped: the import keeps going when the AI page closes, and the page shows [state] when reopened.
 */
class ModelImporter(
    private val modelsDir: File,
    private val cacheDir: File,
    private val scope: CoroutineScope,
    /** Loads the model once and throws when it can't be used. */
    private val verify: suspend (File) -> Unit,
    private val usableSpace: (File) -> Long = { it.usableSpace },
    private val analytics: Analytics = NoOpAnalytics,
) {
    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    // Clears a staging folder left by an import the app was killed during. Imports wait for it, so it
    // can't delete their files.
    private val cleanup: Job = scope.launch(Dispatchers.IO) { File(modelsDir, STAGING_DIR).deleteRecursively() }

    private var job: Job? = null

    /**
     * Starts importing [name] ([size] bytes) read from [open]. [replacing] is the model file this one
     * replaces, deleted once the new one passes. Does nothing while an import is running.
     */
    fun import(name: String, size: Long, open: () -> RawSource, replacing: String?) {
        if (job?.isActive == true) return
        if (!name.endsWith(EXTENSION, ignoreCase = true)) {
            _state.value = ImportState.Failed(ImportError.NotAModel)
            return
        }
        modelsDir.mkdirs()
        val needed = size + HEADROOM_BYTES
        val available = usableSpace(modelsDir)
        if (needed > available) {
            _state.value = ImportState.Failed(ImportError.NoSpace(needed = needed, available = available))
            return
        }
        _state.value = ImportState.Copying(name, 0, size)
        job = scope.launch(Dispatchers.IO) {
            cleanup.join()
            val staging = File(modelsDir, STAGING_DIR).apply { mkdirs() }
            val part = File(staging, "$name.part")
            val checking = File(staging, name)
            // Set only after the staging folder is gone, so a finished state never sees leftovers
            var end: ImportState = ImportState.Idle
            try {
                copy(open, part, name, size)
                Files.move(part.toPath(), checking.toPath(), StandardCopyOption.REPLACE_EXISTING)
                _state.value = ImportState.Checking(name)
                // LiteRT names its weight cache after the file, so a same-name model's cache must not be reused
                clearCache(name)
                val loadError = try {
                    verify(checking)
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    e
                }
                if (loadError != null) {
                    clearCache(name)
                    end = ImportState.Failed(ImportError.WontLoad(loadError.message))
                    return@launch
                }
                Files.move(checking.toPath(), File(modelsDir, name).toPath(), StandardCopyOption.REPLACE_EXISTING)
                if (replacing != null && replacing != name) remove(replacing)
                analytics.log(AiModelInstalled(engine = "local", via = AiModelInstalled.IMPORT))
            } catch (e: CancellationException) {
                throw e
            } catch (_: NotAModelException) {
                end = ImportState.Failed(ImportError.NotAModel)
            } catch (e: Exception) {
                end = ImportState.Failed(ImportError.CopyFailed(e.message))
            } finally {
                withContext(NonCancellable) { staging.deleteRecursively() }
                _state.value = end
            }
        }
    }

    private suspend fun copy(open: () -> RawSource, part: File, name: String, total: Long) {
        val buffer = ByteArray(CHUNK_BYTES)
        var copied = 0L
        open().buffered().use { input ->
            // LiteRT can abort the whole process on a broken file (CheckJNI rejects its error message),
            // so a file without the format's header never reaches it
            val header = if (input.request(MAGIC.size.toLong())) input.peek().readByteArray(MAGIC.size) else ByteArray(0)
            if (!header.contentEquals(MAGIC)) throw NotAModelException()
            part.outputStream().use { output ->
                while (true) {
                    kotlin.coroutines.coroutineContext.ensureActive()
                    val read = input.readAtMostTo(buffer, 0, buffer.size)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    copied += read
                    _state.value = ImportState.Copying(name, copied, total)
                }
            }
        }
    }

    /** Stops a running copy or check; its files are deleted. */
    fun cancel() {
        job?.cancel()
    }

    fun dismissError() {
        if (_state.value is ImportState.Failed) _state.value = ImportState.Idle
    }

    /** Deletes [fileName] from the models folder and the weight cache LiteRT wrote for it. */
    fun remove(fileName: String) {
        File(modelsDir, fileName).delete()
        clearCache(fileName)
    }

    private fun clearCache(fileName: String) {
        cacheDir.listFiles().orEmpty().filter { it.name.startsWith(fileName) }.forEach { it.deleteRecursively() }
    }

    private class NotAModelException : Exception()

    companion object {
        /** Every `.litertlm` file starts with these bytes. */
        private val MAGIC = "LITERTLM".encodeToByteArray()
        const val EXTENSION = ".litertlm"
        const val STAGING_DIR = ".import"

        /** Room left free after the copy, so the weight cache and the app still have space. */
        const val HEADROOM_BYTES = 200L * 1024 * 1024
        private const val CHUNK_BYTES = 1024 * 1024
    }
}
