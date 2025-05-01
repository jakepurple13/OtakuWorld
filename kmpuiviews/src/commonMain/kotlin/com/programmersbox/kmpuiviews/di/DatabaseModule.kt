package com.programmersbox.kmpuiviews.di

import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.kmpuiviews.databaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

val databases: Module = module {
    includes(databaseBuilder)
    single { ItemDatabase.getInstance(get()) }
    single { BlurHashDatabase.getInstance(get()) }
    single { HistoryDatabase.getInstance(get()) }
    single { ListDatabase.getInstance(get()) }
    single { get<ListDatabase>().listDao() }
    single { get<ItemDatabase>().itemDao() }
    single { get<BlurHashDatabase>().blurDao() }
    single { get<HistoryDatabase>().historyDao() }
}