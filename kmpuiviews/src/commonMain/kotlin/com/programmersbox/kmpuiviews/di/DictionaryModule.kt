package com.programmersbox.kmpuiviews.di

import com.programmersbox.favoritesdatabase.DictionaryRepository
import com.programmersbox.favoritesdatabase.StubTranslationService
import com.programmersbox.favoritesdatabase.TranslationService
import org.koin.core.module.Module
import org.koin.dsl.module

val dictionaryModule: Module = module {
    single<TranslationService> { StubTranslationService() }
    single { DictionaryRepository(get(), get()) }
}
