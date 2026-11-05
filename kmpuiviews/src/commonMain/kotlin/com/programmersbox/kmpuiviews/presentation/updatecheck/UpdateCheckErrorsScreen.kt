package com.programmersbox.kmpuiviews.presentation.updatecheck

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.domain.updatecheck.FailureKind
import com.programmersbox.kmpuiviews.domain.updatecheck.ItemFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.domain.updatecheck.failureMessage
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.DateTimeFormatItem
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import com.programmersbox.kmpuiviews.utils.LocalSourcesRepository
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.ImageLoaderChoice
import com.programmersbox.kmpuiviews.utils.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.uc_errors_title
import otakuworld.kmpuiviews.generated.resources.uc_errors_when
import otakuworld.kmpuiviews.generated.resources.uc_group_items
import otakuworld.kmpuiviews.generated.resources.uc_no_errors
import otakuworld.kmpuiviews.generated.resources.uc_no_report
import otakuworld.kmpuiviews.generated.resources.uc_open_details
import otakuworld.kmpuiviews.generated.resources.uc_pill_http
import otakuworld.kmpuiviews.generated.resources.uc_pill_source_failed
import otakuworld.kmpuiviews.generated.resources.uc_pill_timed_out
import otakuworld.kmpuiviews.generated.resources.uc_pill_unreadable
import otakuworld.kmpuiviews.generated.resources.uc_retry_failed
import otakuworld.kmpuiviews.generated.resources.uc_share
import otakuworld.kmpuiviews.generated.resources.uc_source_banner_body
import otakuworld.kmpuiviews.generated.resources.uc_source_banner_title
import otakuworld.kmpuiviews.generated.resources.uc_stat_checked
import otakuworld.kmpuiviews.generated.resources.uc_stat_failed
import otakuworld.kmpuiviews.generated.resources.uc_stat_updated
import otakuworld.kmpuiviews.generated.resources.uc_stopped_early
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCheckErrorsScreen(
    viewModel: UpdateCheckErrorsViewModel = koinViewModel(),
) {
    val navActions = LocalNavActions.current
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val canRetry by viewModel.canRetry.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.uc_errors_title)) },
                navigationIcon = {
                    IconButton(onClick = { navActions.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            if (report?.hasProblems == true) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = LocalNavHostPadding.current.calculateBottomPadding())
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = viewModel::share,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(Res.string.uc_share)) }
                    Button(
                        onClick = viewModel::retryFailed,
                        enabled = canRetry,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(Res.string.uc_retry_failed)) }
                }
            }
        }
    ) { padding ->
        val current = report
        when {
            !loaded -> Unit
            current == null -> EmptyState(stringResource(Res.string.uc_no_report), padding)
            else -> LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "summary") {
                    SummaryCard(current, Modifier.animateItem().padding(horizontal = 16.dp))
                }

                current.stoppedEarly?.let { reason ->
                    item(key = "stopped") {
                        Banner(
                            title = stringResource(Res.string.uc_stopped_early),
                            body = reason,
                            modifier = Modifier.animateItem().padding(horizontal = 16.dp)
                        )
                    }
                }

                if (!current.hasProblems) {
                    item(key = "empty") {
                        Text(
                            stringResource(Res.string.uc_no_errors),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                }

                groups.forEach { group ->
                    item(key = "header:${group.source}") {
                        GroupHeader(group, Modifier.animateItem())
                    }

                    group.sourceFailure?.let { failure ->
                        item(key = "banner:${group.source}") {
                            val name = LocalSourcesRepository.current.displayNameFor(group.source)
                            val reason by produceState("", failure) {
                                value = failureMessage(failure.kind, failure.httpCode)
                            }
                            Banner(
                                title = stringResource(Res.string.uc_source_banner_title, name),
                                body = "$reason. ${stringResource(Res.string.uc_source_banner_body)}",
                                modifier = Modifier.animateItem().padding(horizontal = 16.dp)
                            )
                        }
                    }

                    items(group.items, key = { "item:${group.source}:${it.url}" }) { item ->
                        FailureRow(
                            item = item,
                            onOpen = {
                                navActions.navigate(
                                    Screen.DetailsScreen.Details(
                                        title = item.title,
                                        description = "",
                                        url = item.url,
                                        imageUrl = item.imageUrl,
                                        source = item.source,
                                    )
                                )
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String, padding: PaddingValues) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SummaryCard(report: UpdateCheckReport, modifier: Modifier = Modifier) {
    val is24Hour = koinInject<DateTimeFormatHandler>().is24Time()
    val finished = DateTimeFormatItem(is24Hour).format(report.finishedAt.toLocalDateTime())
    val took = (report.finishedAt - report.startedAt).milliseconds.inWholeSeconds.seconds.toString()

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                stringResource(Res.string.uc_errors_when, finished, took),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat(report.checkedCount, stringResource(Res.string.uc_stat_checked))
                Stat(report.updatedCount, stringResource(Res.string.uc_stat_updated))
                Stat(
                    report.itemFailures.size,
                    stringResource(Res.string.uc_stat_failed),
                    color = if (report.itemFailures.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun Stat(value: Int, label: String, color: Color = Color.Unspecified) {
    Column {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineSmall.merge(TabularNumbers),
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GroupHeader(group: FailureGroup, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        Text(
            LocalSourcesRepository.current.displayNameFor(group.source),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (group.sourceFailure != null) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ) {
            Text(
                kindLabel(group),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Box(Modifier.weight(1f))
        Text(
            pluralStringResource(Res.plurals.uc_group_items, group.items.size, group.items.size),
            style = MaterialTheme.typography.labelMedium.merge(TabularNumbers),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun kindLabel(group: FailureGroup): String = stringResource(
    when {
        group.sourceFailure != null -> Res.string.uc_pill_source_failed
        group.dominantKind == FailureKind.TimedOut -> Res.string.uc_pill_timed_out
        group.dominantKind == FailureKind.Http -> Res.string.uc_pill_http
        else -> Res.string.uc_pill_unreadable
    }
)

@Composable
private fun Banner(title: String, body: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FailureRow(
    item: ItemFailure,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(item.url) { mutableStateOf(false) }
    val message by produceState("", item) { value = item.message() }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item.detail.isNotBlank()) { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        ImageLoaderChoice(
            imageUrl = item.imageUrl,
            name = item.title,
            placeHolder = { painterLogo() },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(40.dp)
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.extraSmall)
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (expanded) {
                Text(
                    item.detail,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        IconButton(onClick = onOpen) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(Res.string.uc_open_details)
            )
        }
    }
}
