package com.programmersbox.koogintegration.embedding

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val embeddingPlatformModule: Module = module {
    single<EmbeddingStorage> { JvmEmbeddingStorage() }
    singleOf(::DesktopEmbeddingRefresher)
}
