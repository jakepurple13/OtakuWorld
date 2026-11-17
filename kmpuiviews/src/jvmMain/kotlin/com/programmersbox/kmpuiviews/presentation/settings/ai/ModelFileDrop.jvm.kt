package com.programmersbox.kmpuiviews.presentation.settings.ai

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import java.net.URI

actual val supportsModelFileDrop: Boolean = true

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.modelFileDrop(onHover: (Boolean) -> Unit, onFile: (PlatformFile) -> Unit): Modifier {
    val hover by rememberUpdatedState(onHover)
    val drop by rememberUpdatedState(onFile)
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = hover(true)
            override fun onExited(event: DragAndDropEvent) = hover(false)
            override fun onEnded(event: DragAndDropEvent) = hover(false)

            override fun onDrop(event: DragAndDropEvent): Boolean {
                hover(false)
                val uri = (event.dragData() as? DragData.FilesList)?.readFiles()?.firstOrNull() ?: return false
                drop(PlatformFile(File(URI(uri))))
                return true
            }
        }
    }
    return dragAndDropTarget(shouldStartDragAndDrop = { it.dragData() is DragData.FilesList }, target = target)
}
