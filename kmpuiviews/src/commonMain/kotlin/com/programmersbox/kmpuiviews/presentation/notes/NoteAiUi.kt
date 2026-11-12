package com.programmersbox.kmpuiviews.presentation.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.DiffSpan
import com.programmersbox.kmpuiviews.domain.aitext.RewriteStyle
import com.programmersbox.kmpuiviews.presentation.components.ai.Spark
import com.programmersbox.kmpuiviews.presentation.components.ai.generatingBorder
import com.programmersbox.kmpuiviews.presentation.components.ai.generatingOutlineColors
import kotlinx.coroutines.coroutineScope
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
import otakuworld.kmpuiviews.generated.resources.ai_writing_tools

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

    // Engines resolve after the sheet opens, so the spark fades in rather than popping
    AnimatedVisibility(
        visible = available,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.6f),
        exit = fadeOut(tween(150)),
    ) {
        Box {
            val toolsLabel = stringResource(Res.string.ai_writing_tools)
            IconButton(onClick = { expanded = true }) {
                Spark(spinning = generating, modifier = Modifier.semantics { contentDescription = toolsLabel })
            }
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
    val deletedWidth = remember { Animatable(1f) }
    val insertedAlpha = remember { Animatable(1f) }
    var accepting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Leaving the screen cancels generation and drops any preview
    DisposableEffect(ai) { onDispose { ai.discard() } }

    LaunchedEffect(preview == null) {
        if (preview == null) {
            deletedAlpha.snapTo(1f)
            deletedWidth.snapTo(1f)
            insertedAlpha.snapTo(1f)
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
                    deletedWidth = deletedWidth.value,
                    insertedAlpha = insertedAlpha.value,
                    showLabel = !accepting,
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
                    // The flowing border takes the outline's place while generating
                    colors = generatingOutlineColors(generating),
                    modifier = Modifier
                        .fillMaxSize()
                        .generatingBorder(generating, OutlinedTextFieldDefaults.shape)
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                )
            }
        }
        // Expands and shrinks too, so the field above resizes smoothly instead of jumping
        AnimatedVisibility(
            visible = preview != null && !generating,
            enter = slideInVertically { it / 3 } + fadeIn(tween(320)) + expandVertically(tween(320)),
            exit = slideOutVertically { it / 3 } + fadeOut(tween(200)) + shrinkVertically(tween(200)),
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
                                // Struck-out words fade first, then they collapse while the green fades
                                // to clear. By the end the preview looks like the plain accepted text.
                                deletedAlpha.animateTo(0f, tween(250))
                                coroutineScope {
                                    launch { deletedWidth.animateTo(0f, tween(400)) }
                                    insertedAlpha.animateTo(0f, tween(400))
                                }
                                ai.accept()?.let(onValueChange)
                            }
                        },
                    ) { Text(stringResource(Res.string.ai_accept)) }
                }
            }
        }
    }
}

/**
 * The finished result. Its text box matches the [OutlinedTextField] it replaces (same text style,
 * 16dp padding, 1dp outline), so once the accept animation ends the swap back is a visual no-op.
 */
@Composable
private fun PreviewText(
    preview: NotePreview,
    deletedAlpha: Float,
    deletedWidth: Float,
    insertedAlpha: Float,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val textStyle = LocalTextStyle.current
    val normalFg = MaterialTheme.colorScheme.onSurface
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
                    // The text eases to the normal color as its highlight clears, so it never fades out
                    is DiffSpan.Inserted -> withStyle(
                        SpanStyle(
                            background = insertedBg.copy(alpha = insertedAlpha),
                            color = lerp(normalFg, insertedFg, insertedAlpha),
                        )
                    ) { append(span.text) }

                    // Shrinking the glyphs collapses the word's width, so the line closes up gradually
                    is DiffSpan.Deleted -> if (deletedWidth > 0f) withStyle(
                        SpanStyle(
                            background = deletedBg.copy(alpha = deletedAlpha),
                            color = deletedFg.copy(alpha = deletedAlpha),
                            textDecoration = TextDecoration.LineThrough,
                            fontSize = deletedWidth.em,
                            letterSpacing = if (textStyle.letterSpacing.isSp) {
                                (textStyle.letterSpacing.value * deletedWidth).sp
                            } else {
                                textStyle.letterSpacing
                            },
                        )
                    ) { append(span.text) }
                }
            }
        }

        state is AiTextState.Done -> AnnotatedString(state.text)
        else -> AnnotatedString(preview.original)
    }
    // Starts hidden so the label grows in when the preview appears instead of pushing the field down
    val labelState = remember { MutableTransitionState(false) }
    labelState.targetState = showLabel && preview.diff != null
    Column(modifier) {
        AnimatedVisibility(
            visibleState = labelState,
            enter = expandVertically(tween(320)) + fadeIn(tween(320)),
            // Fades with the struck-out words, then gives its space back while they collapse
            exit = fadeOut(tween(250)) + shrinkVertically(tween(400, delayMillis = 250)),
        ) {
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
            Text(text, style = textStyle, color = normalFg)
        }
    }
}
