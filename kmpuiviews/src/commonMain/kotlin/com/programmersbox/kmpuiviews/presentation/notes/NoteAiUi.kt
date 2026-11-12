package com.programmersbox.kmpuiviews.presentation.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.DiffSpan
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import com.programmersbox.kmpuiviews.presentation.components.ai.generatingBorder
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.ai_accept
import otakuworld.kmpuiviews.generated.resources.ai_discard
import otakuworld.kmpuiviews.generated.resources.ai_error
import otakuworld.kmpuiviews.generated.resources.ai_fix_grammar
import otakuworld.kmpuiviews.generated.resources.ai_grammar_fixes
import otakuworld.kmpuiviews.generated.resources.ai_note_too_long
import otakuworld.kmpuiviews.generated.resources.ai_rewrite
import otakuworld.kmpuiviews.generated.resources.ai_style_elaborate
import otakuworld.kmpuiviews.generated.resources.ai_style_emojify
import otakuworld.kmpuiviews.generated.resources.ai_style_friendly
import otakuworld.kmpuiviews.generated.resources.ai_style_professional
import otakuworld.kmpuiviews.generated.resources.ai_style_rephrase
import otakuworld.kmpuiviews.generated.resources.ai_style_shorten

private fun RewriteStyle.label(): StringResource = when (this) {
    RewriteStyle.Rephrase -> Res.string.ai_style_rephrase
    RewriteStyle.Shorten -> Res.string.ai_style_shorten
    RewriteStyle.Elaborate -> Res.string.ai_style_elaborate
    RewriteStyle.Friendly -> Res.string.ai_style_friendly
    RewriteStyle.Professional -> Res.string.ai_style_professional
    RewriteStyle.Emojify -> Res.string.ai_style_emojify
}

/** Spark button for a note's top bar. Spins while generating; opens the fix grammar / rewrite menu. */
@Composable
fun NoteAiMenuButton(ai: NoteAiAssistant, text: String) {
    var expanded by remember { mutableStateOf(false) }
    val enabled by ai.toolsEnabled.collectAsState()
    val reason by ai.disabledReason.collectAsState()
    val preview by ai.preview.collectAsState()
    val available by ai.available.collectAsState()
    val generating = preview?.state.let { it is AiTextState.Loading || it is AiTextState.Streaming }

    LaunchedEffect(Unit) { ai.resolveEngines(text) }
    LaunchedEffect(text) { ai.onTextChanged(text) }
    if (!available) return

    Box {
        IconButton(onClick = { expanded = true }) { Spark(spinning = generating) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (!enabled && reason != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.ai_note_too_long)) },
                    onClick = {},
                    enabled = false,
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.ai_fix_grammar)) },
                enabled = enabled,
                onClick = {
                    expanded = false
                    ai.fixGrammar(text)
                },
            )
            Text(
                stringResource(Res.string.ai_rewrite),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            RewriteStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = { Text(stringResource(style.label())) },
                    enabled = enabled,
                    onClick = {
                        expanded = false
                        ai.rewrite(text, style)
                    },
                )
            }
        }
    }
}

/**
 * The note's text field plus the AI preview. While generating the field is read-only, dimmed and
 * wrapped in the flowing border; once done the diff or rewrite is shown with a review bar.
 */
@Composable
fun AiNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    ai: NoteAiAssistant,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val preview by ai.preview.collectAsState()
    val state = preview?.state
    val generating = state is AiTextState.Loading || state is AiTextState.Streaming
    // A rewrite streams its new text into the field; everything else dims the original while it works
    val streamed = (state as? AiTextState.Streaming)?.takeIf { preview?.label != NoteAiAssistant.FIX_GRAMMAR_LABEL }?.text
    val textAlpha by animateFloatAsState(
        if (generating && streamed == null) 0.35f else 1f, tween(300), label = "noteTextAlpha"
    )
    val deletedAlpha = remember { Animatable(1f) }
    val insertedAlpha = remember { Animatable(1f) }
    var deletedGone by remember { mutableStateOf(false) }
    var accepting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Leaving the screen cancels generation and drops any preview
    DisposableEffect(ai) { onDispose { ai.discard() } }

    LaunchedEffect(preview == null) {
        if (preview == null) {
            deletedAlpha.snapTo(1f)
            insertedAlpha.snapTo(1f)
            deletedGone = false
            accepting = false
        }
    }

    Column(modifier) {
        Box(Modifier.weight(1f)) {
            val shown = preview
            if (shown != null && (state is AiTextState.Done || state is AiTextState.Error)) {
                PreviewText(
                    preview = shown,
                    deletedAlpha = deletedAlpha.value,
                    insertedAlpha = insertedAlpha.value,
                    deletedGone = deletedGone,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                OutlinedTextField(
                    value = streamed ?: value,
                    onValueChange = onValueChange,
                    readOnly = generating,
                    enabled = enabled,
                    placeholder = { Text(placeholder) },
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .generatingBorder(generating, OutlinedTextFieldDefaults.shape)
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                )
            }
        }
        AnimatedVisibility(
            visible = preview != null && !generating,
            enter = slideInVertically { it / 3 } + fadeIn(tween(320)),
            exit = slideOutVertically { it / 3 } + fadeOut(tween(200)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state is AiTextState.Error) {
                    Text(
                        stringResource(Res.string.ai_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(onClick = { ai.discard() }, enabled = !accepting) {
                    Text(stringResource(Res.string.ai_discard))
                }
                if (state is AiTextState.Done) {
                    Button(
                        enabled = !accepting,
                        onClick = {
                            accepting = true
                            scope.launch {
                                // Struck-out words fade and go first, then the green fades out
                                deletedAlpha.animateTo(0f, tween(250))
                                deletedGone = true
                                insertedAlpha.animateTo(0f, tween(400))
                                ai.accept()?.let(onValueChange)
                            }
                        },
                    ) { Text(stringResource(Res.string.ai_accept)) }
                }
            }
        }
    }
}

@Composable
private fun PreviewText(
    preview: NotePreview,
    deletedAlpha: Float,
    insertedAlpha: Float,
    deletedGone: Boolean,
    modifier: Modifier = Modifier,
) {
    val insertedBg = MaterialTheme.colorScheme.tertiaryContainer
    val insertedFg = MaterialTheme.colorScheme.onTertiaryContainer
    val deletedBg = MaterialTheme.colorScheme.errorContainer
    val deletedFg = MaterialTheme.colorScheme.onErrorContainer
    val state = preview.state
    val text: AnnotatedString = when {
        state is AiTextState.Done && preview.diff != null -> buildAnnotatedString {
            preview.diff.forEach { span ->
                when (span) {
                    is DiffSpan.Same -> append(span.text)
                    is DiffSpan.Inserted -> withStyle(
                        SpanStyle(background = insertedBg.copy(alpha = insertedAlpha), color = insertedFg)
                    ) { append(span.text) }

                    is DiffSpan.Deleted -> if (!deletedGone) withStyle(
                        SpanStyle(
                            background = deletedBg.copy(alpha = deletedAlpha),
                            color = deletedFg.copy(alpha = deletedAlpha),
                            textDecoration = TextDecoration.LineThrough,
                        )
                    ) { append(span.text) }
                }
            }
        }

        state is AiTextState.Done -> AnnotatedString(state.text)
        else -> AnnotatedString(preview.original)
    }
    Column(modifier) {
        if (preview.diff != null) {
            Text(
                stringResource(Res.string.ai_grammar_fixes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), OutlinedTextFieldDefaults.shape)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
