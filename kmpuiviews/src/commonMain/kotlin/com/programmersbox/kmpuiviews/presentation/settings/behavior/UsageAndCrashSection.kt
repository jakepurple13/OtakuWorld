package com.programmersbox.kmpuiviews.presentation.settings.behavior

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.asState
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupListItem
import com.programmersbox.telemetry.Analytics
import com.programmersbox.telemetry.CrashReporter
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.telemetry_analytics
import otakuworld.kmpuiviews.generated.resources.telemetry_analytics_desc
import otakuworld.kmpuiviews.generated.resources.telemetry_analytics_sends
import otakuworld.kmpuiviews.generated.resources.telemetry_crash
import otakuworld.kmpuiviews.generated.resources.telemetry_crash_desc
import otakuworld.kmpuiviews.generated.resources.telemetry_crash_next_start
import otakuworld.kmpuiviews.generated.resources.telemetry_crash_sends
import otakuworld.kmpuiviews.generated.resources.telemetry_firebase_adds
import otakuworld.kmpuiviews.generated.resources.telemetry_full_details
import otakuworld.kmpuiviews.generated.resources.telemetry_full_details_desc
import otakuworld.kmpuiviews.generated.resources.telemetry_incognito_note
import otakuworld.kmpuiviews.generated.resources.telemetry_never_email
import otakuworld.kmpuiviews.generated.resources.telemetry_never_files
import otakuworld.kmpuiviews.generated.resources.telemetry_never_links
import otakuworld.kmpuiviews.generated.resources.telemetry_never_lists
import otakuworld.kmpuiviews.generated.resources.telemetry_never_notes
import otakuworld.kmpuiviews.generated.resources.telemetry_never_searches
import otakuworld.kmpuiviews.generated.resources.telemetry_never_sent
import otakuworld.kmpuiviews.generated.resources.telemetry_never_titles
import otakuworld.kmpuiviews.generated.resources.telemetry_section
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_ai_engine
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_build_type
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_commit
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_counts
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_country
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_current_screen
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_current_source
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_device
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_durations
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_features
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_instance
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_language
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_os
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_screens
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_settings
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_sources
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_stack
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_steps
import otakuworld.kmpuiviews.generated.resources.telemetry_sent_version

private const val TELEMETRY_DOC_URL = "https://github.com/jakepurple13/OtakuWorld/blob/develop/TELEMETRY.md"

private val analyticsSends = listOf(
    Res.string.telemetry_sent_features, Res.string.telemetry_sent_screens, Res.string.telemetry_sent_sources,
    Res.string.telemetry_sent_settings, Res.string.telemetry_sent_counts, Res.string.telemetry_sent_durations,
    Res.string.telemetry_sent_ai_engine,
)

private val crashSends = listOf(
    Res.string.telemetry_sent_stack, Res.string.telemetry_sent_steps, Res.string.telemetry_sent_current_screen,
    Res.string.telemetry_sent_current_source, Res.string.telemetry_sent_version, Res.string.telemetry_sent_commit,
    Res.string.telemetry_sent_build_type,
)

private val firebaseAdds = listOf(
    Res.string.telemetry_sent_device, Res.string.telemetry_sent_os, Res.string.telemetry_sent_language,
    Res.string.telemetry_sent_country, Res.string.telemetry_sent_instance,
)

private val neverSent = listOf(
    Res.string.telemetry_never_titles, Res.string.telemetry_never_links, Res.string.telemetry_never_searches,
    Res.string.telemetry_never_notes, Res.string.telemetry_never_lists, Res.string.telemetry_never_email,
    Res.string.telemetry_never_files,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
/** Opt-out switches plus everything telemetry sends. Renders nothing in builds without a telemetry backend. */
@Composable
fun UsageAndCrashSection(
    analytics: Analytics = koinInject(),
    crashReporter: CrashReporter = koinInject(),
    dataStore: DataStoreHandling = koinInject(),
) {
    if (!analytics.isAvailable && !crashReporter.isAvailable) return

    var shareAnalytics by dataStore.shareUsageAnalytics.asState()
    var sendCrashes by dataStore.sendCrashReports.asState()
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.telemetry_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        CategoryGroupListItem {
            segmentedListItem(
                onClick = { shareAnalytics = !shareAnalytics },
                leadingContent = { Icon(Icons.Default.BarChart, null) },
                supportingContent = { Text(stringResource(Res.string.telemetry_analytics_desc)) },
                trailingContent = { Switch(checked = shareAnalytics, onCheckedChange = { shareAnalytics = it }) },
            ) { Text(stringResource(Res.string.telemetry_analytics)) }
            segmentedListItem(
                onClick = { sendCrashes = !sendCrashes },
                leadingContent = { Icon(Icons.Default.BugReport, null) },
                supportingContent = { Text(stringResource(Res.string.telemetry_crash_desc)) },
                trailingContent = { Switch(checked = sendCrashes, onCheckedChange = { sendCrashes = it }) },
            ) { Text(stringResource(Res.string.telemetry_crash)) }
        }

        AnimatedVisibility(
            visible = !sendCrashes,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Footnote(stringResource(Res.string.telemetry_crash_next_start))
        }

        ChipGroup(Res.string.telemetry_analytics_sends, analyticsSends, active = shareAnalytics)
        ChipGroup(Res.string.telemetry_crash_sends, crashSends, active = sendCrashes)
        ChipGroup(Res.string.telemetry_firebase_adds, firebaseAdds, active = shareAnalytics || sendCrashes)
        ChipGroup(Res.string.telemetry_never_sent, neverSent, active = true, never = true)
        Footnote(stringResource(Res.string.telemetry_incognito_note))

        CategoryGroupListItem {
            segmentedListItem(
                onClick = { uriHandler.openUri(TELEMETRY_DOC_URL) },
                leadingContent = { Icon(Icons.Default.Description, null) },
                supportingContent = { Text(stringResource(Res.string.telemetry_full_details_desc)) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
            ) { Text(stringResource(Res.string.telemetry_full_details)) }
        }
    }
}

@Composable
private fun Footnote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    title: StringResource,
    items: List<StringResource>,
    active: Boolean,
    never: Boolean = false,
) {
    val groupAlpha by animateFloatAsState(if (active) 1f else 0.4f, label = "telemetryChipGroupAlpha")
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .alpha(groupAlpha)
            .padding(horizontal = 4.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { item ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (never) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer,
                    border = if (never) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                ) {
                    Text(
                        stringResource(item),
                        style = MaterialTheme.typography.labelMedium,
                        textDecoration = if (never) TextDecoration.LineThrough else null,
                        color = if (never) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
