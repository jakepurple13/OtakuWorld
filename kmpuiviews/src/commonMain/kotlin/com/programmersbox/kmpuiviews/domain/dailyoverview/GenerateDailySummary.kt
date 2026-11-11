package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.fold

/** The cached summary, only when it is for the same date as [facts]. Any other date is stale. */
internal suspend fun DailyOverviewSettings.cachedFor(facts: DailyFacts): DailySummary? =
    cached()?.takeIf { it.date == facts.date }

/** Quiet days and Template resolutions skip the engine and use template text. */
internal fun DailyFacts.needsEngine(engine: SummaryEngine): Boolean =
    !isQuiet && engine.id != SummaryEngineId.Template

/** A failed cache write must not discard a summary that already finished. */
internal suspend fun DailyOverviewSettings.cacheOrIgnore(summary: DailySummary) {
    try {
        cache(summary)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    }
}

/**
 * Today's summary for [facts]: the cache when it is for the same date, otherwise a fresh run of the
 * chosen engine. Only a successful run of a real engine is cached. A quiet day, a Template result
 * or an engine failure ends in template text and is not cached, so the next run can try again.
 */
suspend fun generateDailySummary(
    facts: DailyFacts,
    resolver: EngineResolver,
    settings: DailyOverviewSettings,
    template: TemplateEngine,
    bypassCache: Boolean,
): DailySummary {
    if (!bypassCache) {
        settings.cachedFor(facts)?.let { return it }
    }
    val templateSummary = suspend {
        DailySummary(facts.date, SummaryEngineId.Template, template.summarize(facts).first())
    }

    val engine = resolver.resolve(settings.prefs.first().engine)
    if (!facts.needsEngine(engine)) return templateSummary()

    val text = try {
        engine.summarize(facts).fold("") { acc, chunk -> acc + chunk }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        return templateSummary()
    }
    if (text.isBlank()) return templateSummary()
    return DailySummary(facts.date, engine.id, text).also { settings.cacheOrIgnore(it) }
}
