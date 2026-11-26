package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.notes.AiNoteField
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import com.programmersbox.kmpuiviews.presentation.notes.NoteAiMenuButton
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

val NotesPanelWidth: Dp = 280.dp

/** The panel shows only while its tab is on a Details page. Leaving hides it; coming back restores it. */
fun notesPanelVisible(notesOpen: Boolean, current: NavKey): Boolean =
    notesOpen && current is Screen.DetailsScreen.Details

/** Details notes are stored under the item's own URL; the key carries it URL-encoded. */
fun notesItemUrl(key: NavKey): String? = (key as? Screen.DetailsScreen.Details)?.url?.let {
    runCatching { UrlEncoderUtil.decode(it) }.getOrDefault(it)
}

fun TabState.toggleNotes() {
    notesOpen.value = !notesOpen.value
}

/**
 * The tab's notes column beside its pages. Full motion animates the width; Reduced snaps the width and
 * crossfades over 150 ms; Off is instant.
 */
@Composable
fun NotesPanelSlot(tab: TabState, motion: TabMotionState) {
    val visible = notesPanelVisible(tab.notesOpen.value, tab.current)
    // Keeps the content alive while the width runs down after the page changes
    var lastUrl by remember { mutableStateOf<String?>(null) }
    var lastTitle by remember { mutableStateOf("") }
    notesItemUrl(tab.current)?.let {
        lastUrl = it
        lastTitle = (tab.current as Screen.DetailsScreen.Details).title
    }
    val itemUrl = lastUrl

    val level = motion.level
    val widthSpec = if (level == TabMotionLevel.Full) {
        tween<Dp>(motion.ms(TabMotion.SlotOpen), easing = motion.easing(TabMotion.SlotOpen))
    } else snap()
    val width by animateDpAsState(if (visible) NotesPanelWidth else 0.dp, widthSpec)
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        if (level == TabMotionLevel.Reduced) tween(ReducedMotionMs) else snap(),
    )

    if (itemUrl != null && (visible || width > 0.dp || alpha > 0f)) {
        // In Reduced the width snaps, so the fade-out keeps the column until alpha reaches 0
        val shown = if (level == TabMotionLevel.Reduced && alpha > 0f) NotesPanelWidth else width
        Row(Modifier.width(shown).fillMaxHeight().clipToBounds().graphicsLayer { this.alpha = alpha }) {
            VerticalDivider()
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.requiredWidth(NotesPanelWidth)) {
                NotesPanel(itemUrl, lastTitle, onClose = { tab.notesOpen.value = false })
            }
        }
    }
}

/**
 * The note editor as a column. It uses the same [DetailsNotesViewModel] as Details' note sheet, so the first
 * save creates the note. The ViewModel comes from the tab's store (the tab provides `LocalViewModelStoreOwner`),
 * keyed by [itemUrl], so it lives until the tab closes. Text is saved when the panel leaves composition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesPanel(itemUrl: String, itemTitle: String = "", onClose: () -> Unit = {}) {
    val vm: DetailsNotesViewModel = koinViewModel(key = "notes-panel:$itemUrl") { parametersOf(itemUrl, itemTitle) }
    val note by vm.note.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf("") }
    // Only typed text is saved, so a note that has not loaded yet is never overwritten by an empty field
    var edited by rememberSaveable { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(note) {
        if (!edited) text = note?.content.orEmpty()
    }
    DisposableEffect(Unit) {
        onDispose { if (edited) vm.saveNote(text) }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    edited = false
                    text = ""
                    vm.deleteNote()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(itemTitle.ifBlank { "Note" }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
                },
                actions = {
                    NoteAiMenuButton(vm.ai, text)
                    if (note != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete note")
                        }
                    }
                },
            )
        },
    ) { padding ->
        AiNoteField(
            value = text,
            onValueChange = { text = it; edited = true },
            ai = vm.ai,
            placeholder = "Write a note…",
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        )
    }
}
