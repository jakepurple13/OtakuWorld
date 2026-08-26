package com.programmersbox.kmpuiviews.presentation.bookmarks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.kmpuiviews.repository.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BookmarkSortOrder { DATE_DESC, DATE_ASC, TITLE_AZ, PARENT_AZ }

fun String.toFtsQuery(): String =
    trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.joinToString(" ") { "$it*" }

fun List<BookmarkedChapter>.sortedByOrder(order: BookmarkSortOrder): List<BookmarkedChapter> =
    when (order) {
        BookmarkSortOrder.DATE_DESC -> sortedByDescending { it.timestamp }
        BookmarkSortOrder.DATE_ASC -> sortedBy { it.timestamp }
        BookmarkSortOrder.TITLE_AZ -> sortedBy { it.chapterName }
        BookmarkSortOrder.PARENT_AZ -> sortedBy { it.parentTitle }
    }

fun List<BookmarkedChapter>.groupByManga(): Map<String, List<BookmarkedChapter>> =
    groupBy { it.parentTitle }

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkChaptersViewModel(
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    var sortOrder by mutableStateOf(BookmarkSortOrder.DATE_DESC)

    var searchQuery: String
        get() = _searchQuery.value
        set(value) { _searchQuery.value = value }

    val bookmarks: StateFlow<Map<String, List<BookmarkedChapter>>> =
        combine(
            _searchQuery.flatMapLatest { q ->
                if (q.isBlank()) bookmarkRepository.getAllBookmarks()
                else bookmarkRepository.searchBookmarks(q.toFtsQuery())
            },
            snapshotFlow { sortOrder },
        ) { list, sort -> list.sortedByOrder(sort).groupByManga() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap(),
            )

    fun removeBookmark(chapterUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkRepository.deleteBookmark(chapterUrl)
        }
    }
}
