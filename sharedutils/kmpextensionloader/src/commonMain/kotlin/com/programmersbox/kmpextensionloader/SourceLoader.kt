package com.programmersbox.kmpextensionloader

expect class SourceLoader {
    fun load()

    suspend fun blockingLoad()
}