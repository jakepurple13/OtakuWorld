package com.programmersbox.anime.shared.subtitles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Subtitle style stored as JSON in [file]; a missing or unreadable file means defaults. */
class JvmSubtitleSettingsRepository(private val file: File) : SubtitleSettingsRepository {
    private val state = MutableStateFlow(decodeSubtitleStyle(runCatching { file.readText() }.getOrNull()))
    private val writeLock = Mutex()

    override val style: Flow<SubtitleStyle> = state.asStateFlow()

    override suspend fun update(transform: (SubtitleStyle) -> SubtitleStyle) = writeLock.withLock {
        save(transform(state.value).coerced())
    }

    override suspend fun reset() = writeLock.withLock { save(SubtitleStyle()) }

    private suspend fun save(style: SubtitleStyle) {
        withContext(Dispatchers.IO) {
            file.absoluteFile.parentFile?.mkdirs()
            // Write a sibling and move it over, so a crash mid-write never leaves a truncated file.
            val temp = File(file.absoluteFile.parentFile, "${file.name}.tmp")
            temp.writeText(encodeSubtitleStyle(style))
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
        state.value = style
    }
}
