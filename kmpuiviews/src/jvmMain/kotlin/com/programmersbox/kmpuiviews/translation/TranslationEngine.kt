package com.programmersbox.kmpuiviews.translation

enum class EngineKind { Ollama, Custom }

interface TranslationEngine {
    val kind: EngineKind

    /** Codes this engine can translate into, given what is installed/reachable now. */
    suspend fun targets(): List<String>

    /** Throws [TranslationUnavailable] with a user-facing message when it can't. */
    suspend fun translate(text: String, source: String, target: String): String
}

class TranslationUnavailable(message: String) : Exception(message)
