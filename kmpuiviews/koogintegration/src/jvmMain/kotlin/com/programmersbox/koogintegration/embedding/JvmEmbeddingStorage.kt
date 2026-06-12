package com.programmersbox.koogintegration.embedding

import ca.gosyer.appdirs.AppDirs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class JvmEmbeddingStorage(
    private val appDirs: AppDirs,
) : EmbeddingStorage {
    private val file = File(appDirs.getUserDataDir(), "koog_favorite_embeddings.json")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()
    }

    override suspend fun write(content: String): Unit = withContext(Dispatchers.IO) {
        File(appDirs.getUserDataDir()).mkdirs()
        file.writeText(content)
    }
}
