package com.programmersbox.kmpuiviews.domain.moodtags

import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.MoodTagRow
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.MoodInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Moods on titles, stored on this device only. Moods are picked by hand, created by the user, or
 * suggested by the AI when the user asks. Names compare case-insensitively.
 */
class MoodRepository(
    private val dao: ItemDao,
    private val aiText: AiText,
) {
    private val serializer = ListSerializer(String.serializer())

    // Every edit is read-modify-write; without this, fast taps overwrite each other
    private val writeLock = Mutex()

    fun moods(url: String): Flow<List<String>> = dao.moodsFlow(url).map { decode(it?.moods) }

    /** url → moods, current favorites only. */
    val favoriteMoods: Flow<Map<String, List<String>>> =
        dao.favoriteMoodRows().map { rows -> rows.associate { it.url to decode(it.moods) } }

    /** The user's own moods still used by a favorite, alphabetical. */
    val customMoods: Flow<List<String>> = dao.favoriteMoodRows().map { rows ->
        rows.flatMap { decode(it.moods) }
            .filterNot { Moods.isBuiltIn(it) }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    suspend fun toggle(url: String, mood: String) = update(url) { current ->
        if (current.any { it.equals(mood, ignoreCase = true) }) current.without(mood) else current + mood
    }

    suspend fun remove(url: String, mood: String) = update(url) { it.without(mood) }

    /**
     * Adds the user's own mood to [url]. A name matching a built-in or an existing custom mood reuses
     * that one. Returns false when the name is blank.
     */
    suspend fun create(url: String, raw: String): Boolean {
        val name = Moods.cleanCustom(raw) ?: return false
        val canonical = (Moods.builtIn + customMoods.first()).firstOrNull { it.equals(name, ignoreCase = true) } ?: name
        update(url) { current -> if (current.none { it.equals(canonical, ignoreCase = true) }) current + canonical else current }
        return true
    }

    /**
     * Asks the AI for up to 3 moods from the built-in ones plus the user's own, and adds them to [url].
     * Returns false when there's no engine, the AI fails, or it picks nothing from the list.
     */
    suspend fun suggest(url: String, title: String, description: String): Boolean {
        val choices = Moods.builtIn + customMoods.first()
        val raw = try {
            aiText.run(AiFeature.MoodTags, MoodInput(title, description, choices)).fold("") { acc, delta -> acc + delta }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return false
        }
        val picked = AiFeature.MoodTags.finish(raw).lines()
            .mapNotNull { pick -> choices.firstOrNull { it.equals(pick, ignoreCase = true) } }
            .distinct()
            .take(MAX_SUGGESTED)
        if (picked.isEmpty()) return false
        // Read the moods again under the lock: the user may have edited them while the AI worked
        update(url) { current -> current + picked.filter { pick -> current.none { it.equals(pick, ignoreCase = true) } } }
        return true
    }

    private suspend fun update(url: String, change: (List<String>) -> List<String>) = writeLock.withLock {
        set(url, change(decode(dao.moodsOnce(url)?.moods)))
    }

    private suspend fun set(url: String, moods: List<String>) {
        val unique = moods.distinctBy { it.lowercase() }
        if (unique.isEmpty()) {
            dao.deleteMoods(url)
        } else {
            dao.upsertMoods(MoodTagRow(url, Json.encodeToString(serializer, unique), Clock.System.now().toEpochMilliseconds()))
        }
    }

    private fun decode(json: String?): List<String> =
        json?.let { runCatching { Json.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()

    private fun List<String>.without(mood: String) = filterNot { it.equals(mood, ignoreCase = true) }

    private companion object {
        const val MAX_SUGGESTED = 3
    }
}
