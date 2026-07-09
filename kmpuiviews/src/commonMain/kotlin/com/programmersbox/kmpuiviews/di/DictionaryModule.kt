package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.repository.DictionaryRepository
import com.programmersbox.kmpuiviews.repository.StubTranslationService
import com.programmersbox.kmpuiviews.repository.TranslationService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dictionaryModule: Module = module {
    single<TranslationService> { StubTranslationService() }
    singleOf(::DictionaryRepository)
}
