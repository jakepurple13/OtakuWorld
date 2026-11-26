package com.programmersbox.kmpuiviews.tabs

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
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
import com.programmersbox.kmpuiviews.presentation.notes.NoteDetailScreen
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
    notesItemUrl(tab.current)?.let { lastUrl = it }
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
                NotesPanel(itemUrl, onClose = { tab.notesOpen.value = false })
            }
        }
    }
}

/**
 * The Notes detail screen as a column. Its ViewModel comes from the tab's store (the tab provides
 * `LocalViewModelStoreOwner`) keyed by [itemUrl], so it lives until the tab closes.
 */
@Composable
fun NotesPanel(itemUrl: String, onClose: () -> Unit = {}) {
    NoteDetailScreen(
        onBackPress = onClose,
        vm = koinViewModel(key = "notes-panel:$itemUrl") { parametersOf(itemUrl) },
    )
}
