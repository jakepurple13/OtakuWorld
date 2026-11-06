package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.domain.ModelState
import com.programmersbox.kmpuiviews.domain.TranslationLanguage
import com.programmersbox.kmpuiviews.domain.TranslationModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.translation_after_delete
import otakuworld.kmpuiviews.generated.resources.translation_bulk_body
import otakuworld.kmpuiviews.generated.resources.translation_bulk_summary
import otakuworld.kmpuiviews.generated.resources.translation_cancel
import otakuworld.kmpuiviews.generated.resources.translation_cd_back
import otakuworld.kmpuiviews.generated.resources.translation_cd_cancel_download
import otakuworld.kmpuiviews.generated.resources.translation_cd_clear_search
import otakuworld.kmpuiviews.generated.resources.translation_cd_delete
import otakuworld.kmpuiviews.generated.resources.translation_cd_download
import otakuworld.kmpuiviews.generated.resources.translation_cd_locked
import otakuworld.kmpuiviews.generated.resources.translation_chip_required
import otakuworld.kmpuiviews.generated.resources.translation_chip_target
import otakuworld.kmpuiviews.generated.resources.translation_clear
import otakuworld.kmpuiviews.generated.resources.translation_delete
import otakuworld.kmpuiviews.generated.resources.translation_delete_body
import otakuworld.kmpuiviews.generated.resources.translation_delete_count
import otakuworld.kmpuiviews.generated.resources.translation_delete_title
import otakuworld.kmpuiviews.generated.resources.translation_done
import otakuworld.kmpuiviews.generated.resources.translation_download_failed
import otakuworld.kmpuiviews.generated.resources.translation_download_progress
import otakuworld.kmpuiviews.generated.resources.translation_downloaded
import otakuworld.kmpuiviews.generated.resources.translation_downloading
import otakuworld.kmpuiviews.generated.resources.translation_for_languages
import otakuworld.kmpuiviews.generated.resources.translation_picker_downloaded
import otakuworld.kmpuiviews.generated.resources.translation_picker_note
import otakuworld.kmpuiviews.generated.resources.translation_search_languages
import otakuworld.kmpuiviews.generated.resources.translation_select
import otakuworld.kmpuiviews.generated.resources.translation_select_all
import otakuworld.kmpuiviews.generated.resources.translation_selected_count
import otakuworld.kmpuiviews.generated.resources.translation_translate_into
import otakuworld.kmpuiviews.generated.resources.translation_used_by_every
import otakuworld.kmpuiviews.generated.resources.translation_used_days_ago
import otakuworld.kmpuiviews.generated.resources.translation_used_today
import kotlin.math.roundToLong

// ---------- formatting ----------

/** One decimal for MB, two for GB (decimal units), prefixed "about " when [estimate]. */
internal fun formatModelSize(bytes: Long, estimate: Boolean = false): String {
    val tenthsOfMb = (bytes / 100_000.0).roundToLong()
    val text = if (tenthsOfMb >= 10_000) {
        val hundredthsOfGb = (bytes / 10_000_000.0).roundToLong()
        "${hundredthsOfGb / 100}.${(hundredthsOfGb % 100).toString().padStart(2, '0')} GB"
    } else {
        "${tenthsOfMb / 10}.${tenthsOfMb % 10} MB"
    }
    return if (estimate) "about $text" else text
}

private const val DAY_MILLIS = 86_400_000L

/** Corner shape for row [index] of [count] in a grouped list: big outer corners, small inner ones. */
internal fun groupShape(index: Int, count: Int): Shape {
    val big = 20.dp
    val small = 6.dp
    val top = if (index == 0) big else small
    val bottom = if (index == count - 1) big else small
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

// ---------- shared bits ----------

@Composable
private fun LanguageAvatar(glyph: String, downloaded: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (downloaded) colors.primaryContainer else colors.surfaceContainerHighest),
    ) {
        Text(
            glyph,
            style = MaterialTheme.typography.titleMedium,
            color = if (downloaded) colors.onPrimaryContainer else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SmallChip(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = container, contentColor = content) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
internal fun LanguageSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(Res.string.translation_search_languages)) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            AnimatedVisibility(query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, stringResource(Res.string.translation_cd_clear_search))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHighest,
            unfocusedContainerColor = colors.surfaceContainerHighest,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------- target card ----------

@Composable
internal fun TargetLanguageCard(
    target: TranslationLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.translation_translate_into),
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                )
                AnimatedContent(
                    targetState = target.englishName,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 })
                    },
                    label = "target",
                ) { Text(it, style = MaterialTheme.typography.titleLarge) }
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

// ---------- storage meter ----------

@Composable
private fun segmentColors(): List<Color> {
    val c = MaterialTheme.colorScheme
    return listOf(
        c.primary,
        c.tertiary,
        c.secondary,
        c.primary.copy(alpha = 0.55f).compositeOver(c.surfaceContainer),
        c.tertiary.copy(alpha = 0.55f).compositeOver(c.surfaceContainer),
    )
}

@Composable
internal fun StorageMeter(
    downloaded: List<TranslationModel>,
    totalBytes: Long,
    selected: Set<String>,
    selectedBytes: Long,
    selecting: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = segmentColors()
    val estimate = downloaded.any { it.sizeIsEstimate }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AnimatedContent(
                targetState = formatModelSize(totalBytes, estimate),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "total",
            ) { Text(it, style = MaterialTheme.typography.headlineSmall) }
            val subtitle = if (selecting && selected.isNotEmpty()) {
                stringResource(Res.string.translation_after_delete, formatModelSize(totalBytes - selectedBytes, estimate))
            } else {
                pluralStringResource(Res.plurals.translation_for_languages, downloaded.size, downloaded.size)
            }
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 })
                },
                label = "subtitle",
                modifier = Modifier.padding(bottom = 3.dp),
            ) {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            downloaded.forEachIndexed { i, model ->
                key(model.language.code) {
                    val targetWeight = ((model.sizeBytes ?: 0L) / 1_000_000f).coerceAtLeast(0.5f)
                    // Starts near zero so a newly downloaded language grows into the bar.
                    val weight = remember { Animatable(0.001f) }
                    LaunchedEffect(targetWeight) {
                        weight.animateTo(targetWeight, spring(stiffness = Spring.StiffnessLow))
                    }
                    val alpha by animateFloatAsState(if (model.language.code in selected) 0.25f else 1f, label = "segAlpha")
                    Box(
                        Modifier
                            .weight(weight.value.coerceAtLeast(0.001f))
                            .fillMaxHeight()
                            .graphicsLayer { this.alpha = alpha }
                            .background(colors[i % colors.size])
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            downloaded.forEachIndexed { i, model ->
                key(model.language.code) {
                    val alpha by animateFloatAsState(if (model.language.code in selected) 0.45f else 1f, label = "legendAlpha")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer { this.alpha = alpha },
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors[i % colors.size])
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            model.language.englishName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ---------- downloaded section ----------

@Composable
internal fun DownloadedHeader(
    selecting: Boolean,
    selectedCount: Int,
    canSelect: Boolean,
    allSelected: Boolean,
    onSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 4.dp),
    ) {
        AnimatedContent(
            targetState = if (selecting) {
                stringResource(Res.string.translation_selected_count, selectedCount)
            } else {
                stringResource(Res.string.translation_downloaded)
            },
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 })
            },
            label = "dlHeader",
            modifier = Modifier.weight(1f),
        ) { SectionLabel(it) }

        AnimatedContent(
            targetState = selecting,
            transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
            label = "dlActions",
        ) { isSelecting ->
            if (isSelecting) {
                Row {
                    TextButton(onClick = onSelectAll) {
                        Text(
                            stringResource(if (allSelected) Res.string.translation_clear else Res.string.translation_select_all)
                        )
                    }
                    TextButton(onClick = onDone) { Text(stringResource(Res.string.translation_done)) }
                }
            } else if (canSelect) {
                TextButton(onClick = onSelect) { Text(stringResource(Res.string.translation_select)) }
            }
        }
    }
}

@Composable
private fun usageText(model: TranslationModel, nowMillis: Long): String? = when {
    model.required -> stringResource(Res.string.translation_used_by_every)
    model.lastUsedMillis != null -> {
        val days = ((nowMillis - model.lastUsedMillis) / DAY_MILLIS).toInt().coerceAtLeast(0)
        if (days == 0) {
            stringResource(Res.string.translation_used_today)
        } else {
            pluralStringResource(Res.plurals.translation_used_days_ago, days, days)
        }
    }

    else -> null
}

@Composable
internal fun DownloadedRow(
    model: TranslationModel,
    isTarget: Boolean,
    selecting: Boolean,
    selected: Boolean,
    confirming: Boolean,
    nowMillis: Long,
    shape: Shape,
    onAskDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = confirming && !selecting,
        transitionSpec = {
            fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = true)
        },
        label = "rowConfirm",
        modifier = modifier.clip(shape),
    ) { showConfirm ->
        if (showConfirm) {
            DeleteConfirm(model = model, onCancel = onCancelDelete, onConfirm = onConfirmDelete)
        } else {
            DownloadedRowContent(
                model = model,
                isTarget = isTarget,
                selecting = selecting,
                selected = selected,
                nowMillis = nowMillis,
                onAskDelete = onAskDelete,
                onLongPress = onLongPress,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
private fun DownloadedRowContent(
    model: TranslationModel,
    isTarget: Boolean,
    selecting: Boolean,
    selected: Boolean,
    nowMillis: Long,
    onAskDelete: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val background by animateColorAsState(
        if (selected) colors.primaryContainer.copy(alpha = 0.55f).compositeOver(colors.surfaceContainer) else colors.surfaceContainer,
        label = "rowBg",
    )
    val name = model.language.englishName

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(
                enabled = !model.required,
                onClick = { if (selecting) onToggle() },
                onLongClick = if (selecting) null else {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                },
            )
            .heightIn(min = 64.dp)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
    ) {
        AnimatedContent(
            targetState = selecting,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.6f))
            },
            label = "leading",
        ) { isSelecting ->
            if (isSelecting) {
                val circle by animateColorAsState(
                    if (selected) colors.primary else colors.surfaceContainerHighest,
                    label = "checkCircle",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(circle),
                ) {
                    // Row click toggles; a null callback keeps the checkbox from adding its own touch target.
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                        enabled = !model.required,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.onPrimary,
                            checkmarkColor = colors.primary,
                            uncheckedColor = colors.onSurfaceVariant,
                        ),
                    )
                }
            } else {
                LanguageAvatar(model.language.glyph, downloaded = true)
            }
        }

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                AnimatedVisibility(isTarget, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    SmallChip(stringResource(Res.string.translation_chip_target), colors.primary, colors.onPrimary)
                }
                if (model.required) {
                    SmallChip(
                        stringResource(Res.string.translation_chip_required),
                        colors.secondaryContainer,
                        colors.onSecondaryContainer,
                    )
                }
            }
            val size = formatModelSize(model.sizeBytes ?: 0L, model.sizeIsEstimate)
            Text(
                usageText(model, nowMillis)?.let { "$size · $it" } ?: size,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = !selecting,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            if (model.required) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Lock,
                        stringResource(Res.string.translation_cd_locked, name),
                        tint = colors.onSurfaceVariant,
                    )
                }
            } else {
                IconButton(onClick = onAskDelete) {
                    Icon(Icons.Default.Delete, stringResource(Res.string.translation_cd_delete, name))
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirm(
    model: TranslationModel,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val name = model.language.englishName
    val title = stringResource(Res.string.translation_delete_title, name)
    val body = stringResource(
        Res.string.translation_delete_body,
        formatModelSize(model.sizeBytes ?: 0L, model.sizeIsEstimate),
        name,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.errorContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(title) }
                append(" ")
                append(body)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onErrorContainer,
        )
        ConfirmActions(
            confirmText = stringResource(Res.string.translation_delete),
            onCancel = onCancel,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun ConfirmActions(confirmText: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(
            onClick = onCancel,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.onErrorContainer),
        ) { Text(stringResource(Res.string.translation_cancel)) }
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
        ) { Text(confirmText) }
    }
}

// ---------- available section ----------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AvailableRow(
    model: TranslationModel,
    shape: Shape,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val state = model.state
    val downloading = state as? ModelState.Downloading
    val name = model.language.englishName
    val sizeBytes = model.sizeBytes ?: 0L

    // Keep the last progress around so the bar doesn't flip to indeterminate while it collapses.
    val lastProgress = remember { mutableStateOf<ModelState.Downloading?>(null) }
    if (downloading != null) SideEffect { lastProgress.value = downloading }
    val shownProgress = downloading ?: lastProgress.value

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceContainer)
            .heightIn(min = 64.dp)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
    ) {
        LanguageAvatar(model.language.glyph, downloaded = false)

        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = when {
                downloading?.fraction != null -> stringResource(
                    Res.string.translation_download_progress,
                    formatModelSize((sizeBytes * downloading.fraction).toLong(), model.sizeIsEstimate),
                    formatModelSize(sizeBytes, model.sizeIsEstimate),
                )

                downloading != null -> stringResource(Res.string.translation_downloading)
                state is ModelState.Failed -> stringResource(Res.string.translation_download_failed, state.message)
                else -> formatModelSize(sizeBytes, model.sizeIsEstimate)
            }
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = if (state is ModelState.Failed) colors.error else colors.onSurfaceVariant,
            )
            AnimatedVisibility(
                visible = downloading != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val fraction = shownProgress?.fraction
                val progressModifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                if (fraction == null) {
                    LinearWavyProgressIndicator(modifier = progressModifier)
                } else {
                    val animated by animateFloatAsState(fraction, label = "dlProgress")
                    LinearWavyProgressIndicator(progress = { animated }, modifier = progressModifier)
                }
            }
        }

        AnimatedContent(
            targetState = downloading != null,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.8f))
            },
            label = "dlAction",
        ) { isDownloading ->
            if (isDownloading) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, stringResource(Res.string.translation_cd_cancel_download))
                }
            } else {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, stringResource(Res.string.translation_cd_download, name))
                }
            }
        }
    }
}

// ---------- bulk delete bar ----------

private data class BulkBarData(val names: List<String>, val bytes: Long, val estimate: Boolean, val confirming: Boolean)

@Composable
internal fun BulkDeleteBar(
    visible: Boolean,
    selectedModels: List<TranslationModel>,
    selectedBytes: Long,
    confirming: Boolean,
    onAsk: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = BulkBarData(
        names = selectedModels.map { it.language.englishName },
        bytes = selectedBytes,
        estimate = selectedModels.any { it.sizeIsEstimate },
        confirming = confirming,
    )
    // Keep the last visible contents while the bar slides away, instead of "0 selected".
    val lastVisible = remember { mutableStateOf(current) }
    if (visible) SideEffect { lastVisible.value = current }
    val data = if (visible) current else lastVisible.value
    val colors = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = data.confirming,
            transitionSpec = {
                fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = true)
            },
            contentAlignment = Alignment.BottomCenter,
            label = "bulkBar",
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
        ) { isConfirming ->
            val size = formatModelSize(data.bytes, data.estimate)
            if (isConfirming) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.errorContainer,
                    contentColor = colors.onErrorContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        val title = stringResource(Res.string.translation_delete_title, data.names.joinToString(", "))
                        val body = stringResource(Res.string.translation_bulk_body, size)
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(title) }
                                append(" ")
                                append(body)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        ConfirmActions(
                            confirmText = stringResource(Res.string.translation_delete_count, data.names.size),
                            onCancel = onCancel,
                            onConfirm = onConfirm,
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceContainerHighest,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                    ) {
                        Text(
                            stringResource(Res.string.translation_bulk_summary, data.names.size, size),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = onAsk,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.error,
                                contentColor = colors.onError,
                            ),
                        ) { Text(stringResource(Res.string.translation_delete)) }
                    }
                }
            }
        }
    }
}

// ---------- language picker ----------

@Composable
internal fun LanguagePicker(
    languages: List<TranslationModel>,
    targetCode: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        modifier = modifier,
    ) {
        item(key = "picker-header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .animateItem()
                    .padding(bottom = 8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.translation_cd_back))
                }
                Text(stringResource(Res.string.translation_translate_into), style = MaterialTheme.typography.titleLarge)
            }
        }
        item(key = "picker-search") {
            LanguageSearchField(query = query, onQueryChange = onQueryChange, modifier = Modifier.animateItem())
        }
        item(key = "picker-note") {
            Text(
                stringResource(Res.string.translation_picker_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .animateItem()
                    .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 12.dp),
            )
        }
        itemsIndexed(languages, key = { _, m -> m.language.code }) { i, model ->
            val code = model.language.code
            val meta = if (model.state is ModelState.Downloaded) {
                stringResource(Res.string.translation_picker_downloaded, model.language.nativeName)
            } else {
                "${model.language.nativeName} · ${formatModelSize(model.sizeBytes ?: 0L, model.sizeIsEstimate)}"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .animateItem()
                    .padding(top = if (i > 0) 2.dp else 0.dp)
                    .clip(groupShape(i, languages.size))
                    .background(colors.surfaceContainer)
                    .selectable(selected = code == targetCode, role = Role.RadioButton, onClick = { onPick(code) })
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                RadioButton(selected = code == targetCode, onClick = null)
                Column {
                    Text(model.language.englishName, style = MaterialTheme.typography.bodyLarge)
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}
