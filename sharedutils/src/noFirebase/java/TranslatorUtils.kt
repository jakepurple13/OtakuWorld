package com.programmersbox.sharedutils

import android.content.Context

class TranslateItems {
    suspend fun translate(text: String, target: String, onTranslated: suspend (source: String) -> Unit = {}): String = text
    fun clear() = Unit
}

object TranslatorUtils {
    val isAvailable: Boolean = false
    suspend fun downloadedCodes(): Set<String> = emptySet()
    suspend fun download(code: String) = Unit
    suspend fun delete(code: String) = Unit
    fun modelSizeBytes(context: Context, code: String): Long? = null
}
