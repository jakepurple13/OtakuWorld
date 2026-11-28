package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

internal const val REPO_TITLE_TAG = "custom_repo_title"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomReposSheet(
    urls: List<String>,
    statuses: Map<String, CustomRepoStatus>,
    catalogNames: List<String>,
    installedPackages: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRecheck: (String) -> Unit,
    onRecheckAll: () -> Unit,
    onBrowse: (catalogName: String) -> Unit,
    onCopy: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        CustomReposContent(
            urls = urls,
            statuses = statuses,
            catalogNames = catalogNames,
            installedPackages = installedPackages,
            onAdd = onAdd,
            onRemove = onRemove,
            onRecheck = onRecheck,
            onRecheckAll = onRecheckAll,
            onBrowse = onBrowse,
            onCopy = onCopy,
            navigationIcon = { IconButton(onClick = onDismissRequest) { Icon(Icons.Default.Close, "Close") } },
        )
    }
}

/** Top bar, summary, repo cards and the add flow. [navigationIcon] holds the host's close button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomReposContent(
    urls: List<String>,
    statuses: Map<String, CustomRepoStatus>,
    catalogNames: List<String>,
    installedPackages: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRecheck: (String) -> Unit,
    onRecheckAll: () -> Unit,
    onBrowse: (catalogName: String) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Problem repos first; sortedBy is stable so saved order holds otherwise.
    val shown = urls.sortedBy { if (statuses[it].isProblem()) 0 else 1 }

    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            navigationIcon()
            Text(
                "Custom repos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            IconButton(onClick = onRecheckAll) { Icon(Icons.Default.Refresh, "Refresh all") }
        }
        Box(Modifier.fillMaxSize()) {
            RepoList(urls, shown, statuses, catalogNames, installedPackages, expanded, { expanded = it }, onRemove, onRecheck, onBrowse, onCopy, listState)
            ExtendedFloatingActionButton(
                text = { Text("Add repo") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = { adding = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            )
        }
    }

    if (adding) {
        AddRepoSheet(
            catalogNames = catalogNames,
            onDismissRequest = { adding = false },
            onAdd = { draft ->
                adding = false
                val key = repoKey(draft)
                val existing = urls.firstOrNull { repoKey(it) == key }
                if (existing == null) onAdd(draft)
                expanded = existing ?: draft
                // +1 for the summary row.
                if (existing != null) scope.launch { listState.animateScrollToItem(shown.indexOf(existing) + 1) }
            },
        )
    }
}

@Composable
private fun RepoList(
    urls: List<String>,
    shown: List<String>,
    statuses: Map<String, CustomRepoStatus>,
    catalogNames: List<String>,
    installedPackages: Set<String>,
    expanded: String?,
    onExpand: (String?) -> Unit,
    onRemove: (String) -> Unit,
    onRecheck: (String) -> Unit,
    onBrowse: (catalogName: String) -> Unit,
    onCopy: (String) -> Unit,
    state: LazyListState,
) {
    LazyColumn(
        state = state,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "summary") {
            Summary(urls, statuses, Modifier.animateItem())
        }
        items(shown, key = { it }) { url ->
            RepoCard(
                url = url,
                status = statuses[url] ?: CustomRepoStatus.Checking,
                catalogNames = catalogNames,
                installedPackages = installedPackages,
                expanded = expanded == url,
                onToggle = { onExpand(if (expanded == url) null else url) },
                onRemove = { onRemove(url) },
                onRecheck = { onRecheck(url) },
                onBrowse = onBrowse,
                onCopy = { onCopy(url) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

private fun CustomRepoStatus?.isProblem() = this is CustomRepoStatus.Unsupported || this is CustomRepoStatus.Failed

@Composable
private fun Summary(urls: List<String>, statuses: Map<String, CustomRepoStatus>, modifier: Modifier = Modifier) {
    val found = urls.mapNotNull { statuses[it] as? CustomRepoStatus.Found }
    val total = found.sumOf { it.remotes.size }
    val issues = urls.count { statuses[it].isProblem() }
    val error = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                append(total.grouped())
            }
            append(" extensions from ${found.size} ${if (found.size == 1) "repo" else "repos"}")
            if (issues > 0) {
                append(" · ")
                withStyle(SpanStyle(color = error)) {
                    append("$issues ${if (issues == 1) "needs" else "need"} attention")
                }
            }
            append(". Tap a repo for details.")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 2.dp, vertical = 4.dp),
    )
}

@Composable
private fun RepoCard(
    url: String,
    status: CustomRepoStatus,
    catalogNames: List<String>,
    installedPackages: Set<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onRecheck: () -> Unit,
    onBrowse: (String) -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (expanded) colors.surfaceContainerHigh else colors.surfaceContainer)
    val chevron by animateFloatAsState(if (expanded) 180f else 0f)
    val title = (status as? CustomRepoStatus.Found)?.repoName ?: hostOf(url)

    Surface(color = container, shape = RoundedCornerShape(18.dp), modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            ) {
                val (avatarBg, avatarFg) = when {
                    status.isProblem() -> colors.errorContainer to colors.onErrorContainer
                    status is CustomRepoStatus.Found -> catalogColors(status.catalogIndex)
                    else -> colors.primaryContainer to colors.onPrimaryContainer
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(avatarBg),
                ) {
                    Text(
                        title.take(1).uppercase(),
                        color = avatarFg,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag(REPO_TITLE_TAG),
                        )
                        if (status is CustomRepoStatus.Found) CatalogBadge(status.catalogName, status.catalogIndex)
                    }
                    StatusPill(status, catalogNames)
                }
                Icon(Icons.Default.ExpandMore, null, modifier = Modifier.rotate(chevron))
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                ) {
                    Detail(status, catalogNames, installedPackages)
                    Text(
                        url,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurfaceVariant,
                    )
                    checkedAt(status)?.let {
                        Text(
                            "Checked ${relativeTime(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        when (status) {
                            is CustomRepoStatus.Found -> {
                                ActionChip("Browse", Icons.Default.Explore) { onBrowse(status.catalogName) }
                                ActionChip("Refresh", Icons.Default.Refresh, onClick = onRecheck)
                            }

                            is CustomRepoStatus.Failed -> ActionChip("Retry", Icons.Default.Refresh, onClick = onRecheck)
                            else -> Unit
                        }
                        ActionChip("Copy link", Icons.Default.ContentCopy, onClick = onCopy)
                        ActionChip("Remove", Icons.Default.Delete, colors.error, onRemove)
                    }
                }
            }
        }
    }
}

/** Even catalog index = primary, odd = tertiary. */
@Composable
private fun catalogColors(index: Int): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return if (index % 2 == 0) colors.primaryContainer to colors.onPrimaryContainer
    else colors.tertiaryContainer to colors.onTertiaryContainer
}

@Composable
private fun CatalogBadge(name: String, index: Int) {
    val (bg, fg) = catalogColors(index)
    Text(
        name,
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

private enum class PillKind { Ok, Loading, Bad }

@Composable
private fun StatusPill(status: CustomRepoStatus, catalogNames: List<String>) {
    val label = when (status) {
        is CustomRepoStatus.Found -> "${status.remotes.size.grouped()} extensions"
        CustomRepoStatus.Checking -> "Checking ${catalogNames.joinToString(" + ")}…"
        is CustomRepoStatus.Unsupported -> "Not supported"
        is CustomRepoStatus.Failed -> "Couldn't load"
    }
    val kind = when (status) {
        is CustomRepoStatus.Found -> PillKind.Ok
        CustomRepoStatus.Checking -> PillKind.Loading
        else -> PillKind.Bad
    }
    AnimatedContent(
        targetState = label to kind,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { (text, k) ->
        val colors = MaterialTheme.colorScheme
        val fg = when (k) {
            PillKind.Ok -> colors.primary
            PillKind.Loading -> colors.onSurfaceVariant
            PillKind.Bad -> colors.error
        }
        val bg = when (k) {
            PillKind.Loading -> colors.surfaceContainerHighest
            else -> fg.copy(alpha = 0.12f)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            if (k == PillKind.Loading) PulsingDot(fg) else Dot(fg)
            Text(text, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun Dot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun PulsingDot(color: Color) {
    val alpha by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
    )
    Dot(color, Modifier.graphicsLayer { this.alpha = alpha })
}

@Composable
private fun Detail(status: CustomRepoStatus, catalogNames: List<String>, installedPackages: Set<String>) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        is CustomRepoStatus.Found -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Stat(status.remotes.size.grouped(), "extensions", Modifier.weight(1f))
            Stat(status.remotes.count { it.packageName in installedPackages }.grouped(), "installed", Modifier.weight(1f))
            Stat(status.remotes.map { it.lang }.filter { it.isNotBlank() }.distinct().size.grouped(), "languages", Modifier.weight(1f))
        }

        CustomRepoStatus.Checking -> Text(
            "Asking ${if (catalogNames.size > 1) "each catalog" else catalogNames.firstOrNull().orEmpty()} whether it can read this link…",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )

        is CustomRepoStatus.Unsupported -> Text(
            unsupportedText(catalogNames),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.error,
        )

        is CustomRepoStatus.Failed -> Text(
            "Last check failed: ${shortMessage(status.message)}. Check the link or try again later.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.error,
        )
    }
}

private fun unsupportedText(names: List<String>): String {
    val readers = if (names.size > 1) "Neither ${names.joinToString(" nor ")} can" else "${names.firstOrNull().orEmpty()} can't"
    return "$readers read this link. Supported: ${names.joinToString(", ")}."
}

/** First line, at most 120 chars ("…" when cut), without a trailing period. */
private fun shortMessage(message: String): String {
    val line = message.lineSequence().first().trim()
    val cut = if (line.length > 120) line.take(120) + "…" else line
    return cut.trimEnd('.')
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    color: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        colors = if (color == Color.Unspecified) AssistChipDefaults.assistChipColors()
        else AssistChipDefaults.assistChipColors(labelColor = color, leadingIconContentColor = color),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRepoSheet(
    catalogNames: List<String>,
    onDismissRequest: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
        ) {
            Text("Add a repo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Paste a repo link. Supported: ${catalogNames.joinToString(", ")}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("https://…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                ActionChip("Paste", Icons.Default.ContentPaste) {
                    scope.launch { clipboard.plainText()?.let { draft = it.trim() } }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismissRequest) { Text("Cancel") }
                Button(
                    onClick = { draft.trim().takeIf { it.isNotEmpty() }?.let(onAdd) },
                    enabled = draft.isNotBlank(),
                ) { Text("Add") }
            }
        }
    }
}

private fun checkedAt(status: CustomRepoStatus): Instant? = when (status) {
    is CustomRepoStatus.Found -> status.checkedAt
    is CustomRepoStatus.Unsupported -> status.checkedAt
    is CustomRepoStatus.Failed -> status.checkedAt
    CustomRepoStatus.Checking -> null
}

private fun relativeTime(at: Instant): String {
    val elapsed = Clock.System.now() - at
    return when {
        elapsed.inWholeMinutes < 1 -> "just now"
        elapsed.inWholeHours < 1 -> "${elapsed.inWholeMinutes} min ago"
        elapsed.inWholeDays < 1 -> "${elapsed.inWholeHours} h ago"
        else -> "${elapsed.inWholeDays} d ago"
    }
}

/** 1284 -> "1,284". */
private fun Int.grouped(): String = toString().reversed().chunked(3).joinToString(",").reversed()
