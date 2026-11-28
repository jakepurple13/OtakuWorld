package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiTask
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.ErrorDetails
import com.programmersbox.kmpuiviews.domain.exceptions.ParsedException
import com.programmersbox.kmpuiviews.domain.exceptions.hourlyCounts
import com.programmersbox.kmpuiviews.domain.exceptions.knownError
import com.programmersbox.kmpuiviews.domain.exceptions.parseException
import com.programmersbox.kmpuiviews.domain.exceptions.seenCounts
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** One exception as the screen shows it. [id] is the Room key; [time] can repeat. */
data class ExceptionRow(
    val item: ExceptionItem,
    val parsed: ParsedException,
    val source: KmpSourceInformation?,
    /** Installed source name, else the extension id, else null for app errors. */
    val sourceName: String?,
    val extensionVersion: String?,
    val seenThisWeek: Int,
) {
    val id: Long get() = item.id
    val time: Long get() = item.time
}

data class ExceptionDay(val date: LocalDate, val rows: List<ExceptionRow>)

class ExceptionViewModel(
    private val exceptionDao: ExceptionDao,
    private val sourceRepository: SourceRepository,
    private val aiText: AiText,
    private val versionName: (KmpSourceInformation) -> String?,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    private val stored: StateFlow<List<ExceptionItem>> = exceptionDao.getAllExceptions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Ids hidden while their Undo snackbar shows, oldest first. */
    private val pending = MutableStateFlow<List<Long>>(emptyList())

    // Parsing is pure, so parse each stored row once. Main only.
    private val parsedCache = mutableMapOf<Long, ParsedException>()

    // versionName asks the package manager (a binder call on Android); once per package. Main only.
    private val versionCache = mutableMapOf<String, String?>()

    private fun cachedVersion(source: KmpSourceInformation): String? =
        if (versionCache.containsKey(source.packageName)) {
            versionCache[source.packageName]
        } else {
            versionName(source)?.takeIf { it.isNotBlank() }.also { versionCache[source.packageName] = it }
        }

    val rows: StateFlow<List<ExceptionRow>> = combine(stored, pending, sourceRepository.sources) { items, hidden, sources ->
        val ids = items.mapTo(HashSet()) { it.id }
        parsedCache.keys.retainAll(ids)
        val parsed = items.associate { it.id to parsedCache.getOrPut(it.id) { parseException(it.message) } }
        val seen = seenCounts(items.map { it.time to parsed.getValue(it.id).groupKey }, now())
        items
            .filter { it.id !in hidden }
            .sortedByDescending { it.time }
            .map { item ->
                val p = parsed.getValue(item.id)
                val source = p.extension?.let { ext -> sources.firstOrNull { it.packageName == ext.packageName } }
                ExceptionRow(
                    item = item,
                    parsed = p,
                    source = source,
                    sourceName = source?.name ?: p.extension?.id,
                    extensionVersion = source?.let(::cachedVersion),
                    seenThisWeek = seen[p.groupKey] ?: 1,
                )
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val days: StateFlow<List<ExceptionDay>> = rows
        .map { list ->
            list.groupBy { Instant.fromEpochMilliseconds(it.time).toLocalDateTime(timeZone).date }
                .map { (date, dayRows) -> ExceptionDay(date, dayRows) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hourly: StateFlow<List<Int>> = rows
        .map { list -> hourlyCounts(list.map { it.time }, now()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, List(24) { 0 })

    val explainAvailable: StateFlow<Boolean> = aiText.availability(AiFeature.ExplainException)
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val digestAvailable: StateFlow<Boolean> = aiText.availability(AiFeature.ExceptionDigest)
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val digestTask = AiTask(aiText, viewModelScope)
    val digest: StateFlow<AiTextState> = digestTask.state

    // One task per distinct error, so repeats share an answer. Main only.
    private val explanationTasks = mutableMapOf<String, AiTask>()
    private val explanationJobs = mutableMapOf<String, Job>()

    fun row(id: Long): ExceptionRow? = rows.value.firstOrNull { it.id == id }

    fun delete(id: Long) = pending.update { it - id + id }

    fun undo(id: Long) = pending.update { it - id }

    /** Deletes still waiting for Undo, oldest first. A new screen (after rotation) offers Undo again. */
    val pendingIds: StateFlow<List<Long>> = pending

    internal val parsedCacheSize: Int get() = parsedCache.size

    /** Deletes [id] for good, unless it was undone. */
    fun commitDelete(id: Long) {
        if (id !in pending.value) return
        val item = stored.value.firstOrNull { it.id == id }
        viewModelScope.launch {
            if (item != null) exceptionDao.deleteException(item)
            pending.update { it - id }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            exceptionDao.deleteAll()
            pending.value = emptyList()
        }
    }

    /** The row to select after [id] is deleted: the next one, else the previous one. Rows in [skip] (still sliding out) don't count. */
    fun nextAfterDelete(id: Long, skip: Set<Long> = emptySet()): Long? {
        val list = rows.value.filter { it.id == id || it.id !in skip }
        val i = list.indexOfFirst { it.id == id }
        if (i == -1) return null
        return (list.getOrNull(i + 1) ?: list.getOrNull(i - 1))?.id
    }

    fun explanation(row: ExceptionRow): StateFlow<AiTextState> = taskFor(row).state

    /** Starts the AI explanation unless the error is known or already explaining or explained. */
    fun explain(row: ExceptionRow) {
        if (row.parsed.knownError() != null) return
        val key = row.parsed.groupKey
        val task = taskFor(row)
        if (explanationJobs[key]?.isActive == true) return
        when (task.state.value) {
            AiTextState.Idle, is AiTextState.Error -> Unit
            else -> return
        }
        explanationJobs[key] = viewModelScope.launch {
            val started = task.start(AiFeature.ExplainException, ErrorDetails(row.sourceName ?: "the app", row.item.message))
            if (!started) task.fail("No on-device AI engine is ready")
        }
    }

    /** Summarizes the last 24 hours of errors. */
    fun summarize() {
        val cutoff = now() - DAY_MS
        val lines = rows.value
            .filter { it.time >= cutoff }
            .map { "${it.sourceName ?: "App"}: ${it.parsed.shortType} ${it.parsed.firstLine.take(80)}" }
        if (lines.isEmpty()) return
        viewModelScope.launch {
            val started = digestTask.start(AiFeature.ExceptionDigest, lines)
            if (!started) digestTask.fail("No on-device AI engine is ready")
        }
    }

    private fun taskFor(row: ExceptionRow) =
        explanationTasks.getOrPut(row.parsed.groupKey) { AiTask(aiText, viewModelScope) }

    // The deletes onCleared hands to GlobalScope; tests join it before closing the database.
    internal var clearedDeletes: Job? = null

    // viewModelScope is already cancelled here, so finish the deletes the user didn't undo.
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        val ids = pending.value
        if (ids.isEmpty()) return
        val items = stored.value.filter { it.id in ids }
        clearedDeletes = GlobalScope.launch { items.forEach { exceptionDao.deleteException(it) } }
    }

    private companion object {
        const val DAY_MS = 24 * 3_600_000L
    }
}
