package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.RecentModel
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.recent.RecentView
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/** The page ⌘T and "+" open. It has no tab key, so every new tab is its own. */
@Serializable
data object NewTabRoute : NavKey

/**
 * The sections the new tab page shows: for [NewTabShows.Page] the sections that are on, in their saved order; for
 * [NewTabShows.Continue] only Continue reading. Recent and Blank show none of them.
 */
fun newTabContent(shows: NewTabShows, sections: List<SectionSetting>): List<NewTabSection> = when (shows) {
    NewTabShows.Page -> sections.filter { it.on }.map { it.section }
    NewTabShows.Continue -> listOf(NewTabSection.Continue)
    NewTabShows.Recent, NewTabShows.Blank -> emptyList()
}

private data class NewTabSettings(
    val shows: NewTabShows,
    val sections: List<SectionSetting>,
    val continueCount: Int,
    val background: NewTabBackground,
    val focusSearch: Boolean,
)

/** What "A new tab shows" picks: the new tab page, today's Recent screen, Continue reading only, or a blank page. */
@Composable
fun NewTabPage(
    historyDao: HistoryDao = koinInject(),
    itemDao: ItemDao = koinInject(),
) {
    val settingsFlow = remember {
        combine(
            newTabShowsPref.asFlow(),
            newTabSectionsPref.asFlow(),
            newTabContinueCountPref.asFlow(),
            newTabBackgroundPref.asFlow(),
            newTabFocusSearchPref.asFlow(),
            ::NewTabSettings,
        )
    }
    // Null until the saved settings load, so a Blank page never flashes the default page first.
    val settings by settingsFlow.collectAsState(null)
    val current = settings ?: return

    if (current.shows == NewTabShows.Recent) {
        RecentView()
        return
    }

    // Most recently read first.
    val recent by remember(historyDao) { historyDao.getRecentlyViewed().map { it.asReversed() } }
        .collectAsStateWithLifecycle(emptyList<RecentModel>())
    val updates by remember(itemDao) {
        itemDao.getAllNotificationsFlow().map { list -> list.sortedByDescending { it.createdAt }.distinctBy { it.url } }
    }.collectAsStateWithLifecycle(emptyList<NotificationItem>())

    Box(Modifier.fillMaxSize()) {
        if (current.background == NewTabBackground.LastCover) {
            recent.firstOrNull()?.imageUrl?.takeIf { it.isNotBlank() }?.let { LastCoverBackground(it) }
        }

        if (current.shows == NewTabShows.Blank) {
            Image(painterLogo(), null, modifier = Modifier.align(Alignment.Center).size(160.dp).alpha(0.5f))
            return@Box
        }

        val sections = newTabContent(current.shows, current.sections)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (sections.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "All sections are off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            sections.forEach { section ->
                item(key = section.name, contentType = section.name) {
                    val itemModifier = Modifier.animateItem().widthIn(max = 880.dp)
                    when (section) {
                        NewTabSection.Search -> NewTabSearch(focusOnFirstShow = current.focusSearch, modifier = itemModifier)
                        NewTabSection.Continue -> NewTabContinue(recent.take(current.continueCount), itemModifier)
                        NewTabSection.Updates -> NewTabUpdates(updates.take(MAX_UPDATES), itemModifier)
                        NewTabSection.Closed -> NewTabClosed(itemModifier)
                        NewTabSection.Links -> NewTabLinks(itemModifier)
                    }
                }
            }
        }
    }
}

private const val MAX_UPDATES = 8

/** The last-read title's cover, blurred, at 35 % opacity behind the page. Kamel decodes it off the main thread. */
@Composable
private fun LastCoverBackground(url: String) {
    KamelImage(
        resource = { asyncPainterResource(url) },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .blur(48.dp)
            .alpha(0.35f),
    )
}
