package com.programmersbox.koogintegration.embedding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class JvmEmbeddingStorage(
    private val directory: File = File(System.getProperty("user.home"), ".otakuworld"),
) : EmbeddingStorage {
    private val file = File(directory, "koog_favorite_embeddings.json")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()
    }

    override suspend fun write(content: String): Unit = withContext(Dispatchers.IO) {
        directory.mkdirs()
        file.writeText(content)
    }
}
