package com.programmersbox.anime.shared.videos

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
internal actual fun rememberVideoDeleter(): (List<SharedVideoContent>) -> Unit {
    val source = koinInject<VideoLibrarySource>()
    val context = LocalContext.current
    // Android 11+: every video another app owns goes into one system prompt.
    val batch = remember { mutableStateListOf<DeleteAttempt.NeedsConsent>() }
    // Android 10: the system asks for one video at a time.
    val queue = remember { mutableStateListOf<DeleteAttempt.NeedsConsent>() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val allowed = result.resultCode == Activity.RESULT_OK
        if (batch.isNotEmpty()) {
            if (allowed) batch.forEach { source.finishDelete(it.videoFile) }
            batch.clear()
        } else {
            val head = queue.firstOrNull() ?: return@rememberLauncherForActivityResult
            if (allowed) {
                runCatching { context.contentResolver.delete(head.uri, null, null) }.onFailure { it.printStackTrace() }
                source.finishDelete(head.videoFile)
                queue.removeAt(0)
            } else {
                queue.clear()
            }
        }
    }

    val head = queue.firstOrNull()
    LaunchedEffect(head) {
        head?.recoverable?.let {
            launcher.launch(IntentSenderRequest.Builder(it.userAction.actionIntent.intentSender).build())
        }
    }

    val scope = rememberCoroutineScope()
    return remember(source, launcher) {
        { items ->
            scope.launch {
                // File and MediaStore deletes are disk work; the prompts launch back on the main thread.
                val denied = withContext(Dispatchers.IO) { items.map(source::tryDelete) }
                    .filterIsInstance<DeleteAttempt.NeedsConsent>()
                when {
                    denied.isEmpty() -> Unit
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        batch.addAll(denied)
                        val request = MediaStore.createDeleteRequest(context.contentResolver, denied.map { it.uri })
                        launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    }

                    else -> queue.addAll(denied.filter { it.recoverable != null })
                }
            }
            Unit
        }
    }
}

internal actual val libraryDeviceName: String = "phone"

internal actual val otherVideosGoToTrash: Boolean = false

internal actual val otherVideosDeleteNote: String? = "Android may ask you to allow deleting videos other apps saved."

@Composable
internal actual fun OtherVideosPrompt(modifier: Modifier) = Unit

internal actual fun libraryFoldersRoute(): androidx.navigation3.runtime.NavKey? = null
