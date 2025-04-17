package com.programmersbox.uiviews.di

import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.DatabaseBuilder
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDatabase
import org.koin.core.module.Module

fun Module.databases() {
    single { DatabaseBuilder(get()) }
    single { ItemDatabase.getInstance(get()) }
    single { BlurHashDatabase.getInstance(get()) }
    single { HistoryDatabase.getInstance(get()) }
    single { ListDatabase.getInstance(get()) }
    single { get<ListDatabase>().listDao() }
    single { get<ItemDatabase>().itemDao() }
    single { get<BlurHashDatabase>().blurDao() }
    single { get<HistoryDatabase>().historyDao() }
}