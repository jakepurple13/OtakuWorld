package com.programmersbox.kmpuiviews.di

import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.BookmarkDao
import com.programmersbox.favoritesdatabase.BookmarkDatabase
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.ExceptionDatabase
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HeatMapDatabase
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.favoritesdatabase.NotesDao
import com.programmersbox.favoritesdatabase.NotesDatabase
import com.programmersbox.favoritesdatabase.DictionaryDao
import com.programmersbox.favoritesdatabase.DictionaryDatabase
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.favoritesdatabase.RecommendationDatabase
import com.programmersbox.kmpuiviews.databaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

val databases: Module = module {
    includes(databaseBuilder)
    single<ItemDatabase> { ItemDatabase.getInstance(get()) }
    single<ItemDao> { get<ItemDatabase>().itemDao() }
    single<BlurHashDao> { BlurHashDatabase.getInstance(get()).blurDao() }
    single<HistoryDao> { HistoryDatabase.getInstance(get()).historyDao() }
    single<ListDatabase> { ListDatabase.getInstance(get()) }
    single<ListDao> { get<ListDatabase>().listDao() }
    single<RecommendationDao> { RecommendationDatabase.getInstance(get()).recommendationDao() }
    single<HeatMapDao> { HeatMapDatabase.getInstance(get()).heatMapDao() }
    single<ExceptionDao> { ExceptionDatabase.getInstance(get()).exceptionDao() }
    single<BookmarkDatabase> { BookmarkDatabase.getInstance(get()) }
    single<BookmarkDao> { get<BookmarkDatabase>().bookmarkDao() }
    single<NotesDatabase> { NotesDatabase.getInstance(get()) }
    single<NotesDao> { get<NotesDatabase>().notesDao() }
    single<DictionaryDatabase> { DictionaryDatabase.getInstance(get()) }
    single<DictionaryDao> { get<DictionaryDatabase>().dictionaryDao() }
}