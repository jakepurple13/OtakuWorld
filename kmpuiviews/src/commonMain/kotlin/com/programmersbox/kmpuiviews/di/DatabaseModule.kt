package com.programmersbox.kmpuiviews.di

import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.BlurHashDatabase
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.favoritesdatabase.ListDatabase
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.favoritesdatabase.RecommendationDatabase
import com.programmersbox.kmpuiviews.databaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

val databases: Module = module {
    includes(databaseBuilder)
    single<ItemDao> { ItemDatabase.getInstance(get()).itemDao() }
    single<BlurHashDao> { BlurHashDatabase.getInstance(get()).blurDao() }
    single<HistoryDao> { HistoryDatabase.getInstance(get()).historyDao() }
    single<ListDao> { ListDatabase.getInstance(get()).listDao() }
    single<RecommendationDao> { RecommendationDatabase.getInstance(get()).recommendationDao() }
}