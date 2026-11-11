package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_label_template
import otakuworld.kmpuiviews.generated.resources.do_template_failed
import otakuworld.kmpuiviews.generated.resources.do_template_new_chapters
import otakuworld.kmpuiviews.generated.resources.do_template_new_episodes
import otakuworld.kmpuiviews.generated.resources.do_template_quiet
import otakuworld.kmpuiviews.generated.resources.do_template_read_chapters
import otakuworld.kmpuiviews.generated.resources.do_template_read_episodes
import otakuworld.kmpuiviews.generated.resources.do_template_titles

/** No-AI fallback: fixed sentences filled from the facts. */
class TemplateEngine : SummaryEngine {
    override val id = SummaryEngineId.Template
    override val onDevice = true

    override suspend fun label() = getString(Res.string.do_label_template)

    override suspend fun status(): EngineStatus = EngineStatus.Ready

    override fun summarize(facts: DailyFacts): Flow<String> = flow { emit(facts.template()) }

    private suspend fun DailyFacts.template(): String {
        if (isQuiet) return getString(Res.string.do_template_quiet)
        val chapters = unit == UpdateUnit.Chapters
        return listOfNotNull(
            readCount.takeIf { it > 0 }?.let { read ->
                val reads = getPluralString(
                    if (chapters) Res.plurals.do_template_read_chapters else Res.plurals.do_template_read_episodes,
                    read,
                    read,
                )
                // Rows saved before reads were timestamped have no titles.
                val across = titles.size.takeIf { it > 0 }
                    ?.let { getPluralString(Res.plurals.do_template_titles, it, it) }
                    .orEmpty()
                "$reads$across."
            },
            updates.size.takeIf { it > 0 }?.let {
                getPluralString(
                    if (chapters) Res.plurals.do_template_new_chapters else Res.plurals.do_template_new_episodes,
                    it,
                    it,
                )
            },
            failedSources.size.takeIf { it > 0 }
                ?.let { getPluralString(Res.plurals.do_template_failed, it, it) },
        ).joinToString(" ")
    }
}
