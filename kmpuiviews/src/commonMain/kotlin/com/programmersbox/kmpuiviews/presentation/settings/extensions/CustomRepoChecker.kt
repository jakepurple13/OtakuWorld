package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.programmersbox.kmpmodels.KmpExternalCustomApiServicesCatalog
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpmodels.RepoCheck
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

sealed interface CustomRepoStatus {
    data object Checking : CustomRepoStatus

    data class Found(
        val catalogIndex: Int,
        val catalogName: String,
        val repoName: String,
        val remotes: List<KmpRemoteSources>,
        val checkedAt: Instant,
    ) : CustomRepoStatus

    data class Unsupported(val checkedAt: Instant) : CustomRepoStatus

    data class Failed(val message: String, val checkedAt: Instant) : CustomRepoStatus
}

/**
 * Found (first named one in list order, else first blank-named one) > Failed (first message) > Unsupported.
 * A blank name comes from the default [KmpExternalCustomApiServicesCatalog.checkRepo], so a catalog that read the
 * repo itself wins. [url] is only used to name a repo whose catalog reported no name.
 */
fun mergeRepoChecks(
    checks: List<RepoCheck>,
    catalogs: List<KmpExternalCustomApiServicesCatalog>,
    now: Instant,
    url: String = "",
): CustomRepoStatus {
    val foundIndex = checks.indexOfFirst { it is RepoCheck.Found && it.name.isNotBlank() }
        .takeIf { it >= 0 } ?: checks.indexOfFirst { it is RepoCheck.Found }
    if (foundIndex >= 0) {
        val found = checks[foundIndex] as RepoCheck.Found
        return CustomRepoStatus.Found(
            catalogIndex = foundIndex,
            catalogName = catalogs[foundIndex].name,
            repoName = found.name.ifBlank { repoFallbackTitle(url) },
            remotes = found.remotes,
            checkedAt = now,
        )
    }
    val failed = checks.firstOrNull { it is RepoCheck.Failed } as RepoCheck.Failed?
    if (failed != null) return CustomRepoStatus.Failed(failed.message.ifBlank { "Unknown error" }, now)
    return CustomRepoStatus.Unsupported(now)
}

private val githubHosts = setOf("raw.githubusercontent.com", "github.com")

/** Card title for a repo with no reported name: "owner/repo" for GitHub links, otherwise the host. */
internal fun repoFallbackTitle(url: String): String {
    val rest = url.trim().substringAfter("://").substringBefore('?').substringBefore('#')
    val host = rest.substringBefore('/').ifBlank { return url.trim() }
    val segments = rest.substringAfter('/', "").split('/').filter { it.isNotEmpty() }
    return if (host.lowercase() in githubHosts && segments.size >= 2) "${segments[0]}/${segments[1]}" else host
}

/**
 * Trimmed, scheme and host lower-cased (path case kept), trailing "/", "/index.min.json", "/repo.json" removed.
 * Used only for duplicate detection.
 */
fun repoKey(url: String): String {
    val trimmed = url.trim()
    val schemeEnd = trimmed.indexOf("://")
    val hostEnd = if (schemeEnd < 0) -1 else trimmed.indexOf('/', schemeEnd + 3)
    val normalized = when {
        schemeEnd < 0 -> trimmed
        hostEnd < 0 -> trimmed.lowercase()
        else -> trimmed.substring(0, hostEnd).lowercase() + trimmed.substring(hostEnd)
    }
    return normalized
        .trimEnd('/')
        .removeSuffix("/index.min.json")
        .removeSuffix("/repo.json")
        .trimEnd('/')
}

/** The url in [saved] that has the same [repoKey] as [url], or null. */
fun savedRepoMatching(url: String, saved: List<String>): String? {
    val key = repoKey(url)
    return saved.firstOrNull { repoKey(it) == key }
}

class CustomRepoChecker(
    private val catalogs: () -> List<KmpExternalCustomApiServicesCatalog>,
    private val scope: CoroutineScope,
    private val clock: () -> Instant = { Clock.System.now() },
    /** Catalog checks run here, off the caller's (main) thread; status writes stay in [scope]. */
    private val checkDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /** Keyed by saved url. */
    val statuses: SnapshotStateMap<String, CustomRepoStatus> = mutableStateMapOf()
    private val jobs = mutableMapOf<String, Job>()
    private var lastCatalogNames: List<String>? = null

    /**
     * Checks urls with no status and drops statuses for removed urls.
     * When the catalog list changed since the last sync, every url is checked again.
     */
    fun sync(urls: List<String>) {
        val wanted = urls.toSet()
        (statuses.keys - wanted).forEach { removed ->
            jobs.remove(removed)?.cancel()
            statuses.remove(removed)
        }
        // Compared by name: the catalog list is rebuilt with new instances (e.g. per-source bridge wrappers).
        val current = catalogs().map { it.name }
        val catalogsChanged = lastCatalogNames != current
        lastCatalogNames = current
        wanted.filter { catalogsChanged || it !in statuses }.forEach(::check)
    }

    fun recheck(url: String) {
        if (url in statuses) check(url)
    }

    fun recheckAll() = statuses.keys.toList().forEach(::check)

    private fun check(url: String) {
        jobs.remove(url)?.cancel()
        statuses[url] = CustomRepoStatus.Checking
        if (!url.trim().startsWith("https://", ignoreCase = true)) {
            statuses[url] = CustomRepoStatus.Failed("Only https links are supported", clock())
            return
        }
        val snapshot = catalogs()
        jobs[url] = scope.launch {
            val checks = coroutineScope { snapshot.map { async(checkDispatcher) { it.safeCheck(url) } }.awaitAll() }
            // The url may have been removed (and its job cancelled) while the catalogs answered.
            if (url in statuses) statuses[url] = mergeRepoChecks(checks, snapshot, clock(), url)
        }
    }

    private suspend fun KmpExternalCustomApiServicesCatalog.safeCheck(url: String): RepoCheck = try {
        checkRepo(url)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        RepoCheck.Failed(e.message.orEmpty())
    }
}
