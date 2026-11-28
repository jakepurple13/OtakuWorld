package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionHeadline
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionKind
import com.programmersbox.kmpuiviews.domain.exceptions.ParsedException
import com.programmersbox.kmpuiviews.domain.exceptions.knownError
import com.programmersbox.kmpuiviews.domain.updatecheck.KnownError
import com.programmersbox.kmpuiviews.presentation.components.ai.AiCard
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.exc_a_source
import otakuworld.kmpuiviews.generated.resources.exc_explain_blocked_desktop
import otakuworld.kmpuiviews.generated.resources.exc_explain_one
import otakuworld.kmpuiviews.generated.resources.exc_frames_hidden
import otakuworld.kmpuiviews.generated.resources.exc_headline_app
import otakuworld.kmpuiviews.generated.resources.exc_headline_blocked
import otakuworld.kmpuiviews.generated.resources.exc_headline_network
import otakuworld.kmpuiviews.generated.resources.exc_headline_not_found
import otakuworld.kmpuiviews.generated.resources.exc_headline_offline
import otakuworld.kmpuiviews.generated.resources.exc_headline_parsing
import otakuworld.kmpuiviews.generated.resources.exc_headline_rate_limited
import otakuworld.kmpuiviews.generated.resources.exc_headline_server_error
import otakuworld.kmpuiviews.generated.resources.exc_headline_source
import otakuworld.kmpuiviews.generated.resources.exc_headline_timed_out
import otakuworld.kmpuiviews.generated.resources.exc_kind_app
import otakuworld.kmpuiviews.generated.resources.exc_kind_blocked
import otakuworld.kmpuiviews.generated.resources.exc_kind_network
import otakuworld.kmpuiviews.generated.resources.exc_kind_parsing
import otakuworld.kmpuiviews.generated.resources.exc_kind_source
import otakuworld.kmpuiviews.generated.resources.exc_known_error
import otakuworld.kmpuiviews.generated.resources.exc_this_source
import otakuworld.kmpuiviews.generated.resources.exc_what_it_might_mean
import otakuworld.kmpuiviews.generated.resources.exc_what_this_means

/** The shared-transition scopes for single-pane mode. Null in two-pane mode, where nothing flies. */
@OptIn(ExperimentalSharedTransitionApi::class)
class SharedKeys(val scope: SharedTransitionScope, val visibility: AnimatedVisibilityScope)

/** Shared element (or bounds, for containers) under [key] when [keys] is set; a no-op otherwise. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedIf(keys: SharedKeys?, key: String, bounds: Boolean = false): Modifier {
    if (keys == null) return this
    with(keys.scope) {
        val state = rememberSharedContentState(key)
        return if (bounds) {
            this@sharedIf.sharedBounds(state, keys.visibility)
        } else {
            this@sharedIf.sharedElement(state, keys.visibility)
        }
    }
}

/** Text and outline colors for each kind. Blocked uses an amber that isn't in the color scheme. */
@Composable
fun kindColor(kind: ExceptionKind): Color {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    return when (kind) {
        ExceptionKind.Network -> scheme.tertiary
        ExceptionKind.Blocked -> if (dark) Color(0xFFF7BD52) else Color(0xFF8A5A00)
        ExceptionKind.Parsing -> scheme.error
        ExceptionKind.Source, ExceptionKind.App -> scheme.onSurfaceVariant
    }
}

@Composable
fun KindBadge(kind: ExceptionKind, modifier: Modifier = Modifier) {
    val color = kindColor(kind)
    val label = when (kind) {
        ExceptionKind.Network -> Res.string.exc_kind_network
        ExceptionKind.Blocked -> Res.string.exc_kind_blocked
        ExceptionKind.Parsing -> Res.string.exc_kind_parsing
        ExceptionKind.Source -> Res.string.exc_kind_source
        ExceptionKind.App -> Res.string.exc_kind_app
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Plain-language headline, e.g. "MangaDex took too long to answer". */
@Composable
fun headlineText(row: ExceptionRow): String {
    val source = row.sourceName ?: stringResource(Res.string.exc_a_source)
    val res = when (row.parsed.headline) {
        ExceptionHeadline.TimedOut -> Res.string.exc_headline_timed_out
        ExceptionHeadline.Offline -> Res.string.exc_headline_offline
        ExceptionHeadline.Blocked -> Res.string.exc_headline_blocked
        ExceptionHeadline.RateLimited -> Res.string.exc_headline_rate_limited
        ExceptionHeadline.NotFound -> Res.string.exc_headline_not_found
        ExceptionHeadline.ServerError -> Res.string.exc_headline_server_error
        ExceptionHeadline.Network -> Res.string.exc_headline_network
        ExceptionHeadline.Parsing -> Res.string.exc_headline_parsing
        ExceptionHeadline.Source -> Res.string.exc_headline_source
        ExceptionHeadline.App -> Res.string.exc_headline_app
    }
    return stringResource(res, source)
}

/** The trace: error line, the app's and extension's frames, library frames folded, causes. */
@Composable
fun StackTraceBlock(parsed: ParsedException, modifier: Modifier = Modifier) {
    var showLibrary by rememberSaveable(parsed.groupKey) { mutableStateOf(false) }
    val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val hidden = parsed.frames.count { it.isLibrary }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Text(
                    listOf(parsed.type, parsed.message).filter { it.isNotEmpty() }.joinToString(": "),
                    style = mono,
                    color = MaterialTheme.colorScheme.error,
                )
                parsed.frames.filter { showLibrary || !it.isLibrary }.forEach { frame ->
                    Text(
                        "    " + frame.text,
                        style = mono,
                        color = if (frame.isLibrary) muted.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                    )
                }
                if (hidden > 0 && !showLibrary) {
                    Text(
                        "    ▸ " + stringResource(Res.string.exc_frames_hidden, hidden),
                        style = mono,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showLibrary = true },
                    )
                }
                parsed.causes.forEach { cause ->
                    Text(cause, style = mono, color = kindColor(ExceptionKind.Blocked))
                }
            }
        }
    }
}

/**
 * Fixed text for known errors; otherwise the AI card. [autoStart] explains right away (detail
 * page); without it an "Explain this one" chip asks first (timeline rows). Hidden when no engine.
 */
@Composable
fun ExplanationBlock(
    row: ExceptionRow,
    state: AiTextState,
    available: Boolean,
    autoStart: Boolean,
    onExplain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val known = row.parsed.knownError()
    when {
        known != null -> KnownExplanation(known, row.sourceName, modifier)
        !available -> Unit
        autoStart || state != AiTextState.Idle -> {
            if (autoStart) LaunchedEffect(row.parsed.groupKey) { onExplain() }
            AiCard(
                label = stringResource(Res.string.exc_what_it_might_mean),
                state = state,
                onRetry = onExplain,
                modifier = modifier,
            )
        }
        else -> AssistChip(
            onClick = onExplain,
            label = { Text(stringResource(Res.string.exc_explain_one)) },
            leadingIcon = { Spark(spinning = false) },
            modifier = modifier,
        )
    }
}

@Composable
private fun KnownExplanation(known: KnownError, sourceName: String?, modifier: Modifier = Modifier) {
    val source = sourceName ?: stringResource(Res.string.exc_this_source)
    val text = if (known == KnownError.Blocked && !hasShareSheet) {
        stringResource(Res.string.exc_explain_blocked_desktop, source)
    } else {
        stringResource(known.resource, source)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.exc_what_this_means),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    " · " + stringResource(Res.string.exc_known_error),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
