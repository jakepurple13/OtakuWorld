package com.programmersbox.koogintegration.embedding

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidEmbeddingStorage(context: Context) : EmbeddingStorage {
    private val file = File(context.filesDir, "koog_favorite_embeddings.json")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()
    }

    override suspend fun write(content: String): Unit = withContext(Dispatchers.IO) {
        file.writeText(content)
    }
}
