package com.programmersbox.koogintegration.embedding

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module
import org.koin.dsl.module

actual val embeddingPlatformModule: Module = module {
    single<EmbeddingStorage> { AndroidEmbeddingStorage(androidContext()) }
    workerOf(::FavoritesEmbeddingWorker)
}
