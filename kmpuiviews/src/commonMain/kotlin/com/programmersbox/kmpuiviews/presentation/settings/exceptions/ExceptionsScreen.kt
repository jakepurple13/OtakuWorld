package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.appVersion
import com.programmersbox.kmpuiviews.domain.exceptions.ReportContext
import com.programmersbox.kmpuiviews.domain.exceptions.buildExceptionReport
import com.programmersbox.kmpuiviews.domain.exceptions.reportFileName
import com.programmersbox.kmpuiviews.domain.exceptions.reportSubject
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.components.OtakuScaffold
import com.programmersbox.kmpuiviews.presentation.updatecheck.UpdateCheckReportSharer
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.exc_cancel
import otakuworld.kmpuiviews.generated.resources.exc_delete
import otakuworld.kmpuiviews.generated.resources.exc_delete_all_body
import otakuworld.kmpuiviews.generated.resources.exc_delete_all_title
import otakuworld.kmpuiviews.generated.resources.exc_deleted
import otakuworld.kmpuiviews.generated.resources.exc_report_copied
import otakuworld.kmpuiviews.generated.resources.exc_report_saved
import otakuworld.kmpuiviews.generated.resources.exc_title
import otakuworld.kmpuiviews.generated.resources.exc_undo

/** Wide enough for the timeline and the detail side by side. */
private val TWO_PANE_WIDTH = 840.dp

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun ExceptionsScreen(
    viewModel: ExceptionViewModel = koinViewModel(),
) {
    HideNavBarWhileOnScreen()

    val days by viewModel.days.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val hourly by viewModel.hourly.collectAsStateWithLifecycle()
    val digest by viewModel.digest.collectAsStateWithLifecycle()
    val digestAvailable by viewModel.digestAvailable.collectAsStateWithLifecycle()
    val explainAvailable by viewModel.explainAvailable.collectAsStateWithLifecycle()

    val navActions = LocalNavActions.current
    val appConfig = koinInject<AppConfig>()
    val dataStoreHandling = koinInject<DataStoreHandling>()
    val sharer = koinInject<UpdateCheckReportSharer>()
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val version = appVersion()

    var showDeleteAll by remember { mutableStateOf(false) }
    var exiting by remember { mutableStateOf(emptySet<Long>()) }
    val listFocus = remember { FocusRequester() }

    // Predictive back: scrub the pane transition while the gesture is held.
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = navigator.canNavigateBack(),
        onBackCancelled = { scope.launch { navigator.seekBack(fraction = 0f) } },
        onBackCompleted = { scope.launch { navigator.navigateBack() } },
    )
    val backTransition = backState.transitionState
    LaunchedEffect(backTransition) {
        if (backTransition is NavigationEventTransitionState.InProgress) {
            navigator.seekBack(fraction = backTransition.latestEvent.progress)
        }
    }

    fun report(row: ExceptionRow) = buildExceptionReport(
        rawText = row.item.message,
        parsed = row.parsed,
        time = row.time,
        sourceName = row.sourceName,
        extensionVersion = row.extensionVersion,
        seenThisWeek = row.seenThisWeek,
        ctx = ReportContext(appConfig.appName, version, deviceDescription(), timeZone),
    )

    var pendingSave by remember { mutableStateOf<String?>(null) }
    val saver = rememberFileSaverLauncher(dialogSettings = FileKitDialogSettings.createDefault()) { file ->
        val text = pendingSave
        if (file != null && text != null) {
            scope.launch {
                runCatching { file.writeString(text) }
                    .onSuccess { snackbarHostState.showSnackbar(getString(Res.string.exc_report_saved)) }
            }
        }
    }

    val actions = ExceptionActions(
        copy = { row ->
            clipboard.setText(AnnotatedString(report(row)))
            scope.launch { snackbarHostState.showSnackbar(getString(Res.string.exc_report_copied)) }
        },
        shareOrSave = { row ->
            if (hasShareSheet) {
                sharer.share(report(row), reportSubject(appConfig.appName, row.sourceName, row.parsed))
            } else {
                pendingSave = report(row)
                saver.launch(
                    reportFileName(appConfig.appName, row.time, row.sourceName, timeZone).removeSuffix(".md"),
                    "md",
                )
            }
        },
        sourceAction = { row, action ->
            val source = row.source
            when (action) {
                SourceAction.OpenWebsite -> source?.apiService?.baseUrl?.let { url ->
                    if (hasShareSheet) navActions.webView(url) else uriHandler.openUri(url)
                }
                SourceAction.CheckUpdate -> navActions.extensionList()
                SourceAction.Browse -> source?.let {
                    scope.launch {
                        dataStoreHandling.currentService.set(it.apiService.serviceName)
                        navActions.recent()
                    }
                }
                SourceAction.Settings -> source?.let {
                    navActions.navigate(Screen.SourcePreferences(it.apiService.serviceName))
                }
            }
        },
    )

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text(stringResource(Res.string.exc_delete_all_title)) },
            text = { Text(stringResource(Res.string.exc_delete_all_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAll()
                    showDeleteAll = false
                }) { Text(stringResource(Res.string.exc_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) { Text(stringResource(Res.string.exc_cancel)) }
            },
        )
    }

    OtakuScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val twoPane = maxWidth >= TWO_PANE_WIDTH
            val selected = navigator.currentDestination?.contentKey

            // Two panes with nothing picked: show the newest one instead of an empty half.
            LaunchedEffect(twoPane, rows.firstOrNull()?.id, selected) {
                val first = rows.firstOrNull()?.id
                if (twoPane && first != null && (selected == null || viewModel.row(selected) == null)) {
                    navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, first)
                }
            }

            // Delete: move the selection (or leave the detail) first, then slide the row out.
            // The row reports when it's gone; only then is it hidden and the Undo snackbar shown.
            val delete: (Long) -> Unit = { id ->
                scope.launch {
                    if (twoPane) {
                        val next = viewModel.nextAfterDelete(id)
                        if (selected == id && next != null) navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, next)
                    } else if (selected == id) {
                        navigator.navigateBack()
                    }
                    exiting = exiting + id
                }
            }
            val onExited: (Long) -> Unit = { id ->
                exiting = exiting - id
                viewModel.delete(id)
                scope.launch {
                    showUndo(snackbarHostState, onUndo = { viewModel.undo(id) }, onCommit = { viewModel.commitDelete(id) })
                }
            }
            val visibleIds = rows.map { it.id }.filter { it !in exiting }
            val onKey: (KeyEvent) -> Boolean = handler@{ event ->
                if (!twoPane) return@handler false
                val action = exceptionKeyAction(event.toKeyPress()) ?: return@handler false
                val i = visibleIds.indexOf(selected)
                when (action) {
                    ExceptionKeyAction.Up, ExceptionKeyAction.Down -> {
                        val step = if (action == ExceptionKeyAction.Down) 1 else -1
                        val target = visibleIds.getOrNull(
                            (if (i == -1) 0 else i + step).coerceIn(0, (visibleIds.size - 1).coerceAtLeast(0))
                        )
                        if (target != null) scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, target) }
                    }
                    ExceptionKeyAction.Delete -> selected?.let(delete)
                    ExceptionKeyAction.Copy -> selected?.let(viewModel::row)?.let(actions.copy)
                    ExceptionKeyAction.Undo -> snackbarHostState.currentSnackbarData?.performAction()
                }
                true
            }

            SharedTransitionLayout {
                ListDetailPaneScaffold(
                    directive = navigator.scaffoldDirective.copy(maxHorizontalPartitions = if (twoPane) 2 else 1),
                    scaffoldState = navigator.scaffoldState,
                    listPane = {
                        AnimatedPane(enterTransition = fadeIn(), exitTransition = fadeOut()) {
                            if (twoPane) LaunchedEffect(Unit) { runCatching { listFocus.requestFocus() } }
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .focusRequester(listFocus)
                                    .onPreviewKeyEvent(onKey)
                                    .focusable()
                            ) {
                                TopAppBar(
                                    title = { Text(stringResource(Res.string.exc_title)) },
                                    navigationIcon = { BackButton() },
                                    actions = {
                                        IconButton(onClick = { showDeleteAll = true }) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                        }
                                    },
                                )
                                ExceptionTimeline(
                                    days = days,
                                    hourly = hourly,
                                    digest = digest,
                                    digestAvailable = digestAvailable,
                                    explainAvailable = explainAvailable,
                                    twoPane = twoPane,
                                    selected = if (twoPane) selected else null,
                                    onSelect = { id -> scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) } },
                                    onMoreInfo = { id -> scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) } },
                                    onDelete = delete,
                                    onCopy = actions.copy,
                                    onSummarize = viewModel::summarize,
                                    explanation = viewModel::explanation,
                                    onExplain = viewModel::explain,
                                    sharedKeys = if (twoPane) null else SharedKeys(this@SharedTransitionLayout, this@AnimatedPane),
                                    contentPadding = padding,
                                    exiting = exiting,
                                    onExited = onExited,
                                    onSave = actions.shareOrSave,
                                    onOpenWebsite = { row -> actions.sourceAction(row, SourceAction.OpenWebsite) },
                                    modifier = Modifier.weight(1f),
                                )
                                val hint = keyboardHint()
                                if (twoPane && hint != null) {
                                    HorizontalDivider()
                                    Text(
                                        hint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    },
                    detailPane = {
                        AnimatedPane(enterTransition = fadeIn(), exitTransition = fadeOut()) {
                            if (twoPane) {
                                AnimatedContent(
                                    targetState = selected,
                                    transitionSpec = {
                                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { 24 }) togetherWith fadeOut(tween(120))
                                    },
                                ) { id ->
                                    val row = id?.let(viewModel::row)
                                    if (row == null) {
                                        EmptyDetail()
                                    } else {
                                        DetailFor(row, viewModel, explainAvailable, twoPane = true, actions, delete, onBack = {}, sharedKeys = null)
                                    }
                                }
                            } else {
                                val row = selected?.let(viewModel::row)
                                if (row != null) {
                                    DetailFor(
                                        row = row,
                                        viewModel = viewModel,
                                        explainAvailable = explainAvailable,
                                        twoPane = false,
                                        actions = actions,
                                        onDelete = delete,
                                        onBack = { scope.launch { navigator.navigateBack() } },
                                        sharedKeys = SharedKeys(this@SharedTransitionLayout, this@AnimatedPane),
                                        contentModifier = Modifier.animateEnterExit(
                                            enter = fadeIn(tween(250, delayMillis = 120)) +
                                                slideInVertically(tween(250, delayMillis = 120)) { it / 12 },
                                        ),
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

/** Screen-level callbacks both panes use. */
private class ExceptionActions(
    val copy: (ExceptionRow) -> Unit,
    val shareOrSave: (ExceptionRow) -> Unit,
    val sourceAction: (ExceptionRow, SourceAction) -> Unit,
)

@Composable
private fun DetailFor(
    row: ExceptionRow,
    viewModel: ExceptionViewModel,
    explainAvailable: Boolean,
    twoPane: Boolean,
    actions: ExceptionActions,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    sharedKeys: SharedKeys?,
    contentModifier: Modifier = Modifier,
) {
    val explanation by viewModel.explanation(row).collectAsStateWithLifecycle()
    ExceptionDetail(
        row = row,
        explanation = explanation,
        explainAvailable = explainAvailable,
        twoPane = twoPane,
        onBack = onBack,
        onDelete = { onDelete(row.id) },
        onCopy = { actions.copy(row) },
        onShareOrSave = { actions.shareOrSave(row) },
        onExplain = { viewModel.explain(row) },
        onSourceAction = { actions.sourceAction(row, it) },
        sharedKeys = sharedKeys,
        contentModifier = contentModifier,
    )
}

/** "Exception deleted · Undo". Undo brings the row back; anything else deletes it for good. */
internal suspend fun showUndo(host: SnackbarHostState, onUndo: () -> Unit, onCommit: () -> Unit) {
    val result = host.showSnackbar(
        message = getString(Res.string.exc_deleted),
        actionLabel = getString(Res.string.exc_undo),
        duration = SnackbarDuration.Short,
    )
    if (result == SnackbarResult.ActionPerformed) onUndo() else onCommit()
}
