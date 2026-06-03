package com.programmersbox.kmpuiviews.presentation.bookmarks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.kmpuiviews.utils.composables.imageloaders.CustomKamelImage
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    onBackPress: () -> Unit = {},
    vm: BookmarkChaptersViewModel = koinViewModel(),
) {
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = { vm.searchQuery = it },
                placeholder = { Text("Search bookmarks…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BookmarkSortOrder.entries.forEach { sort ->
                    FilterChip(
                        selected = vm.sortOrder == sort,
                        onClick = { vm.sortOrder = sort },
                        label = {
                            Text(
                                when (sort) {
                                    BookmarkSortOrder.DATE_DESC -> "Newest"
                                    BookmarkSortOrder.DATE_ASC -> "Oldest"
                                    BookmarkSortOrder.TITLE_AZ -> "Chapter A–Z"
                                    BookmarkSortOrder.MANGA_AZ -> "Manga A–Z"
                                }
                            )
                        },
                    )
                }
            }
            if (bookmarks.isEmpty()) {
                BookmarksEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    bookmarks.forEach { (mangaTitle, chapters) ->
                        item(key = mangaTitle) {
                            MangaBookmarkGroup(
                                mangaTitle = mangaTitle,
                                chapters = chapters,
                                onRemove = { vm.removeBookmark(it.chapterUrl) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "No bookmarks yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Bookmark chapters from the manga details screen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MangaBookmarkGroup(
    mangaTitle: String,
    chapters: List<BookmarkedChapter>,
    onRemove: (BookmarkedChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }
    val coverUrl = chapters.firstOrNull()?.parentImageUrl.orEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = {
                Text(
                    mangaTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            supportingContent = {
                Text(
                    "${chapters.size} bookmark${if (chapters.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                CustomKamelImage(
                    imageUrl = coverUrl,
                    name = mangaTitle,
                    modifier = Modifier
                        .width(40.dp)
                        .height(56.dp),
                    placeHolder = { rememberVectorPainter(Icons.Filled.Bookmark) },
                    onError = { rememberVectorPainter(Icons.Filled.Bookmark) },
                    contentScale = ContentScale.Crop,
                )
            },
            trailingContent = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            },
            modifier = Modifier.clickable { expanded = !expanded },
        )
        HorizontalDivider()
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                chapters.forEach { bookmark ->
                    BookmarkedChapterRow(
                        bookmark = bookmark,
                        onRemove = { onRemove(bookmark) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkedChapterRow(
    bookmark: BookmarkedChapter,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(bookmark.chapterName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                formatRelativeTime(bookmark.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove bookmark",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        modifier = modifier.padding(start = 16.dp),
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}

@OptIn(ExperimentalTime::class)
private fun formatRelativeTime(timestamp: Long): String {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val diff = nowMs - timestamp
    val minutes = diff / 60_000L
    val hours = diff / 3_600_000L
    val days = diff / 86_400_000L
    return when {
        minutes < 60L -> "${minutes}m ago"
        hours < 24L -> "${hours}h ago"
        days < 7L -> "${days}d ago"
        else -> "${days / 7L}w ago"
    }
}
