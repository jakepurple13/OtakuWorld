package com.programmersbox.kmpuiviews.translation.bergamot

import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.translation.planChain
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * One source language that can be translated into the current target, alone or through English.
 *
 * [sharedBytes] are the chain's files another downloaded row also uses; [ownBytes] the rest.
 * [freedOnDelete] is what deleting this row frees: [ownBytes] when downloaded, else 0.
 */
data class BergamotRow(
    val source: String,
    val target: String,
    val chain: List<BergamotPair>,
    val state: ModelState,
    val ownBytes: Long,
    val sharedBytes: Long,
    val freedOnDelete: Long,
)

/**
 * Bergamot model files on disk under [dir]. Pair files are the storage unit: `<dir>/<from>-<to>/` holds a pair's
 * files plus `pair.json` (written last, so a pair is installed only once every file passed its sha256). A row is
 * a (source, target) the user downloaded, kept in `downloaded.json`; deleting a row removes only pairs no other
 * downloaded row's chain uses.
 *
 * Disk state is read on first use, not in the constructor.
 */
class BergamotModelStore(
    private val index: BergamotIndex,
    private val dir: File,
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    @Serializable
    private data class RowKey(val source: String, val target: String)

    private data class State(
        val target: String = "en",
        val indexPairs: List<BergamotPair> = emptyList(),
        val installed: Map<String, BergamotPair> = emptyMap(),
        val downloaded: Set<RowKey> = emptySet(),
        val progress: Map<RowKey, Float?> = emptyMap(),
        val failures: Map<RowKey, String> = emptyMap(),
    ) {
        /** Installed pairs win over the index, so an index update never strands files already on disk. */
        val pairs: List<BergamotPair> get() = installed.values + indexPairs.filter { it.key !in installed }
    }

    private val state by lazy { MutableStateFlow(readDisk()) }
    private val jobs = ConcurrentHashMap<RowKey, Job>()
    private val pairLocks = ConcurrentHashMap<String, Mutex>()
    private val diskLock = Any()

    private val _indexError = MutableStateFlow<String?>(null)

    /** Why the model list couldn't be loaded (and nothing was cached), or null. */
    val indexError: StateFlow<String?> = _indexError.asStateFlow()

    /** One per source language reachable into the current target. */
    val rows: StateFlow<List<BergamotRow>> by lazy {
        state.map(::computeRows).stateIn(scope, SharingStarted.Eagerly, computeRows(state.value))
    }

    /** Bytes on disk, each file once. */
    val totalBytes: StateFlow<Long> by lazy {
        state.map(::installedBytes).stateIn(scope, SharingStarted.Eagerly, installedBytes(state.value))
    }

    fun setTarget(code: String) = state.update { it.copy(target = code) }

    /** Reloads the model list (falls back to the index's disk cache offline). */
    suspend fun refresh() {
        val pairs = try {
            index.pairs()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _indexError.value = e.message ?: "Couldn't load the model list"
            return
        }
        _indexError.value = null
        state.update { it.copy(indexPairs = pairs) }
    }

    /** Downloads the pairs [source] → current target needs that aren't on disk yet. */
    fun download(source: String) {
        val key = RowKey(source, state.value.target)
        if (jobs[key]?.isActive == true) return
        state.update { it.copy(failures = it.failures - key, progress = it.progress + (key to 0f)) }
        // Lazy so the job is in [jobs] (where delete() looks for chains in use) before it does anything.
        val job = scope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
            try {
                val chain = planChain(key.source, key.target, state.value.pairs)
                    ?: throw IOException("No model for ${bergamotLanguageName(key.source)} → ${bergamotLanguageName(key.target)}")
                val missing = chain.filter { it.key !in state.value.installed }
                val total = missing.sumOf { it.sizeBytes }.coerceAtLeast(1)
                var done = 0L
                for (pair in missing) {
                    pairLocks.getOrPut(pair.key) { Mutex() }.withLock {
                        if (pair.key in state.value.installed) {
                            done += pair.sizeBytes
                        } else {
                            installPair(pair) { bytes ->
                                done += bytes
                                val fraction = (done.toFloat() / total).coerceIn(0f, 1f)
                                state.update { it.copy(progress = it.progress + (key to fraction)) }
                            }
                        }
                    }
                }
                if (chain.any { it.key !in state.value.installed }) {
                    throw IOException("Part of this download was removed. Try again.")
                }
                state.update { it.copy(downloaded = it.downloaded + key) }
                writeDownloaded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(failures = it.failures + (key to (e.message ?: "Download failed"))) }
            } finally {
                state.update { it.copy(progress = it.progress - key) }
            }
        }
        jobs[key] = job
        job.invokeOnCompletion { jobs.remove(key, job) }
        job.start()
    }

    fun cancel(source: String) {
        jobs[RowKey(source, state.value.target)]?.cancel()
    }

    /** Removes the row and every pair of its chain no other downloaded or downloading row uses. */
    suspend fun delete(source: String) = withContext(Dispatchers.IO) {
        val key = RowKey(source, state.value.target)
        jobs[key]?.cancelAndJoin()
        val current = state.value
        val keep = usedPairs(current, current.downloaded + activeDownloads() - key)
        val remove = planChain(key.source, key.target, current.pairs).orEmpty()
            .map { it.key }
            .filter { it in current.installed && it !in keep }
        // Forget the pairs first so nothing new starts using files that are about to go.
        state.update {
            it.copy(
                installed = it.installed - remove.toSet(),
                downloaded = it.downloaded - key,
                failures = it.failures - key,
            )
        }
        writeDownloaded()
        remove.forEach { pairKey ->
            pairLocks.getOrPut(pairKey) { Mutex() }.withLock {
                // A download may have started (and even reinstalled the pair) since the state update above.
                val now = state.value
                if (pairKey !in now.installed && pairKey !in usedPairs(now, activeDownloads())) {
                    File(dir, pairKey).deleteRecursively()
                }
            }
        }
    }

    private fun activeDownloads(): Set<RowKey> = jobs.filterValues { it.isActive }.keys

    /** The model files for each step of [source] → [target] using installed pairs, or null if any are missing. */
    fun filesFor(source: String, target: String): List<BergamotModelFiles>? {
        val installed = state.value.installed
        val chain = planChain(source, target, installed.values.toList()) ?: return null
        return chain.map { modelFiles(it) ?: return null }
    }

    internal fun installedPairs(): List<BergamotPair> = state.value.installed.values.toList()

    private fun computeRows(s: State): List<BergamotRow> {
        val pairs = s.pairs
        val sources = pairs.flatMap { listOf(it.from, it.to) }.toSet() - s.target
        return sources.mapNotNull { source ->
            val chain = planChain(source, s.target, pairs)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val key = RowKey(source, s.target)
            val usedByOthers = usedPairs(s, s.downloaded - key)
            val shared = chain.filter { it.key in usedByOthers }.sumOf { it.sizeBytes }
            val own = chain.sumOf { it.sizeBytes } - shared
            val isDownloaded = key in s.downloaded && chain.all { it.key in s.installed }
            val rowState = when {
                key in s.progress -> ModelState.Downloading(s.progress[key])
                key in s.failures -> ModelState.Failed(s.failures.getValue(key))
                isDownloaded -> ModelState.Downloaded
                else -> ModelState.NotDownloaded
            }
            BergamotRow(
                source = source,
                target = s.target,
                chain = chain,
                state = rowState,
                ownBytes = own,
                sharedBytes = shared,
                freedOnDelete = if (isDownloaded) own else 0,
            )
        }.sortedBy { bergamotLanguageName(it.source) }
    }

    private fun usedPairs(s: State, rows: Set<RowKey>): Set<String> =
        rows.flatMap { planChain(it.source, it.target, s.pairs).orEmpty() }.map { it.key }.toSet()

    private fun installedBytes(s: State): Long = s.installed.values.sumOf { it.sizeBytes }

    /** The sha256 in the name pins the content; Marian picks the vocab loader by extension, so vocabs keep `.spm`. */
    private fun fileFor(file: BergamotFile): File {
        val extension = if (file.type.endsWith("vocab")) "spm" else "bin"
        return File(File(dir, file.pair), "${file.type}-${file.sha256.take(16)}.$extension")
    }

    private fun modelFiles(pair: BergamotPair): BergamotModelFiles? {
        val byType = pair.files.associateBy { it.type }
        val model = byType["model"] ?: return null
        val src = byType["srcvocab"] ?: byType["vocab"] ?: return null
        val trg = byType["trgvocab"] ?: byType["vocab"] ?: return null
        return BergamotModelFiles(
            model = fileFor(model),
            lex = byType["lex"]?.let(::fileFor),
            srcVocab = fileFor(src),
            trgVocab = fileFor(trg),
        )
    }

    /** Fetches each missing file of [pair] to `.part`, checks its sha256, renames it, then writes the manifest. */
    private suspend fun installPair(pair: BergamotPair, onBytes: (Long) -> Unit) {
        File(dir, pair.key).mkdirs()
        for (file in pair.files) {
            val dest = fileFor(file)
            if (dest.isFile) { // finished by an earlier, interrupted attempt; the name pins its sha256
                onBytes(file.sizeBytes)
                continue
            }
            val part = File(dest.path + ".part")
            try {
                val sha256 = fetch(file.url, part, onBytes)
                if (sha256 != file.sha256) throw IOException("The downloaded ${file.type} file is damaged. Try again.")
                if (!part.renameTo(dest)) throw IOException("Couldn't save ${dest.name}")
            } finally {
                part.delete()
            }
        }
        File(File(dir, pair.key), MANIFEST).writeText(json.encodeToString(BergamotPair.serializer(), pair))
        state.update { it.copy(installed = it.installed + (pair.key to pair)) }
    }

    /** Streams [url] into [part]; returns its sha256 as lowercase hex. */
    private suspend fun fetch(url: String, part: File, onBytes: (Long) -> Unit): String =
        client.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) throw IOException("Download failed (HTTP ${response.status.value})")
            val channel = response.bodyAsChannel()
            val digest = MessageDigest.getInstance("SHA-256")
            part.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = channel.readAvailable(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    out.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    onBytes(read.toLong())
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

    private fun readDisk(): State {
        val installed = dir.listFiles().orEmpty()
            .mapNotNull { pairDir ->
                val manifest = File(pairDir, MANIFEST).takeIf { it.isFile } ?: return@mapNotNull null
                runCatching { json.decodeFromString(BergamotPair.serializer(), manifest.readText()) }.getOrNull()
                    ?.takeIf { pair -> pair.files.all { fileFor(it).isFile } }
            }
            .associateBy { it.key }
        val downloaded = File(dir, DOWNLOADED).takeIf { it.isFile }
            ?.let { runCatching { json.decodeFromString<List<RowKey>>(it.readText()) }.getOrNull() }
            .orEmpty()
            .toSet()
        return State(installed = installed, downloaded = downloaded)
    }

    private fun writeDownloaded() = synchronized(diskLock) {
        dir.mkdirs()
        val tmp = File(dir, "$DOWNLOADED.tmp")
        tmp.writeText(json.encodeToString(state.value.downloaded.toList()))
        val dest = File(dir, DOWNLOADED)
        if (!tmp.renameTo(dest)) {
            dest.delete()
            tmp.renameTo(dest)
        }
    }

    private companion object {
        const val MANIFEST = "pair.json"
        const val DOWNLOADED = "downloaded.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}

internal fun bergamotLanguageName(code: String) = TranslationLanguages.byCode(code)?.englishName ?: code
