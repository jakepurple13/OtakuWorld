package com.programmersbox.favoritesdatabase

import androidx.room3.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object FavoritesDatabaseConstructor : RoomDatabaseConstructor<ItemDatabase> {
    override fun initialize(): ItemDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object SettingsDatabaseConstructor : RoomDatabaseConstructor<SettingsDatabase> {
    override fun initialize(): SettingsDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object NotesDatabaseConstructor : RoomDatabaseConstructor<NotesDatabase> {
    override fun initialize(): NotesDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object RecommendationDatabaseConstructor : RoomDatabaseConstructor<RecommendationDatabase> {
    override fun initialize(): RecommendationDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object HistoryDatabaseConstructor : RoomDatabaseConstructor<HistoryDatabase> {
    override fun initialize(): HistoryDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object HeatMapDatabaseConstructor : RoomDatabaseConstructor<HeatMapDatabase> {
    override fun initialize(): HeatMapDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object ExceptionDatabaseConstructor : RoomDatabaseConstructor<ExceptionDatabase> {
    override fun initialize(): ExceptionDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object DictionaryDatabaseConstructor : RoomDatabaseConstructor<DictionaryDatabase> {
    override fun initialize(): DictionaryDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object BookmarkDatabaseConstructor : RoomDatabaseConstructor<BookmarkDatabase> {
    override fun initialize(): BookmarkDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object ListDatabaseConstructor : RoomDatabaseConstructor<ListDatabase> {
    override fun initialize(): ListDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object BlurHashDatabaseConstructor : RoomDatabaseConstructor<BlurHashDatabase> {
    override fun initialize(): BlurHashDatabase
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object SyncPreferencesConstructor : RoomDatabaseConstructor<SyncPreferences> {
    override fun initialize(): SyncPreferences
}