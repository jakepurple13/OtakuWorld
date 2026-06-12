package com.programmersbox.koogintegration.embedding

import org.koin.core.module.Module

/** Platform-specific registrations: EmbeddingStorage + scheduling entry points. */
expect val embeddingPlatformModule: Module
