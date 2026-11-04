package com.programmersbox.anime.shared.videos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/** Desktop settings page for the folders "Other videos on this computer" comes from. */
@Serializable
data object LibraryFoldersRoute : NavKey

@Composable
internal actual fun rememberVideoDeleter(): (List<SharedVideoContent>) -> Unit {
    val source = koinInject<VideoLibrarySource>()
    val scope = rememberCoroutineScope()
    return remember(source) { { items -> scope.launch(Dispatchers.IO) { items.forEach(source::delete) } } }
}

internal actual val libraryDeviceName: String = "computer"

internal actual val otherVideosGoToTrash: Boolean get() = canMoveToTrash

internal actual val otherVideosDeleteNote: String?
    get() = if (canMoveToTrash) null else "Other videos are deleted permanently."

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal actual fun ThumbnailProgressNotice(modifier: Modifier) {
    val source = koinInject<VideoLibrarySource>()
    val pending by source.thumbnailer.pending.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = pending > 0,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Getting thumbnails · $pending left",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "AnimeWorld opens each video once to grab a frame and read its length. The first one " +
                            "is slow while the video player starts up. After that they're saved, so this only " +
                            "happens for new videos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private enum class PromptStage { Ask, Scanning }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal actual fun OtherVideosPrompt(modifier: Modifier) {
    val settings = koinInject<AnimeDesktopSettings>()
    val source = koinInject<VideoLibrarySource>()
    val navActions = LocalNavActions.current
    val scope = rememberCoroutineScope()

    val asked by settings.libraryScanAsked.asFlow().collectAsStateWithLifecycle(true)
    val progress by source.scanProgress.collectAsStateWithLifecycle()
    var stage by remember { mutableStateOf(PromptStage.Ask) }
    var scanFinished by remember { mutableStateOf(false) }

    // After Allow, stay on "Looking for videos…" until the first scan of the library folders ends.
    LaunchedEffect(stage) {
        if (stage == PromptStage.Scanning) {
            withTimeoutOrNull(2_000) { snapshotFlow { progress }.first { it != null } }
            snapshotFlow { progress }.first { it == null }
            scanFinished = true
        }
    }

    AnimatedVisibility(
        visible = !asked || (stage == PromptStage.Scanning && !scanFinished),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = stage,
                transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform() },
                label = "otherVideosPrompt"
            ) { target ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    when (target) {
                        PromptStage.Ask -> {
                            Text("Show every video on this computer?", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "AnimeWorld will look for video files in these folders. Nothing is uploaded. " +
                                    "On macOS you'll be asked once for Downloads and Desktop.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AnimeDesktopSettings.DEFAULT_LIBRARY_FOLDERS.forEach {
                                    AssistChip(onClick = {}, label = { Text(libraryFolderLabel(it)) })
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        stage = PromptStage.Scanning
                                        scope.launch {
                                            settings.includeOtherFolders.set(true)
                                            settings.libraryScanAsked.set(true)
                                        }
                                    }
                                ) { Text("Allow") }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            settings.includeOtherFolders.set(true)
                                            settings.libraryScanAsked.set(true)
                                        }
                                        navActions.navigate(LibraryFoldersRoute)
                                    }
                                ) { Text("Choose folders…") }
                                OutlinedButton(
                                    onClick = { scope.launch { settings.libraryScanAsked.set(true) } }
                                ) { Text("Not now") }
                            }
                        }

                        PromptStage.Scanning -> {
                            Text("Looking for videos…", style = MaterialTheme.typography.titleMedium)
                            val current = progress
                            Text(
                                current?.folder.orEmpty(),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (current != null && current.total > 0) {
                                LinearWavyProgressIndicator(
                                    progress = { current.done.toFloat() / current.total },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

internal actual fun libraryFoldersRoute(): NavKey? = LibraryFoldersRoute
