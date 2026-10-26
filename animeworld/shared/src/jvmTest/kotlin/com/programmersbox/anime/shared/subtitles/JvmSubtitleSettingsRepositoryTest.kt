package com.programmersbox.anime.shared.subtitles

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmSubtitleSettingsRepositoryTest {
    @Test
    fun settingsPersistAcrossInstances() = runTest {
        val dir = Files.createTempDirectory("subtitle-settings").toFile()
        try {
            val file = dir.resolve("nested/subtitle_style.json")
            JvmSubtitleSettingsRepository(file).update {
                it.copy(sizePercent = 150, edge = SubtitleEdge.Shadow, preferredLanguages = listOf("es"), autoEnable = false)
            }

            val reloaded = JvmSubtitleSettingsRepository(file).style.first()
            assertEquals(
                SubtitleStyle(sizePercent = 150, edge = SubtitleEdge.Shadow, preferredLanguages = listOf("es"), autoEnable = false),
                reloaded,
            )

            JvmSubtitleSettingsRepository(file).reset()
            assertEquals(SubtitleStyle(), JvmSubtitleSettingsRepository(file).style.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun unwritableTargetKeepsInMemoryValueWithoutThrowing() = runTest {
        val dir = Files.createTempDirectory("subtitle-settings").toFile()
        try {
            // A regular file where the parent directory should be: mkdirs and the write both fail.
            val blocker = dir.resolve("blocker").apply { writeText("x") }
            val repo = JvmSubtitleSettingsRepository(blocker.resolve("subtitle_style.json"))

            repo.update { it.copy(sizePercent = 150) }

            assertEquals(150, repo.style.first().sizePercent)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun unreadableFileYieldsDefaults() = runTest {
        val file = Files.createTempFile("subtitle-style", ".json").toFile()
        try {
            file.writeText("{not json")
            assertEquals(SubtitleStyle(), JvmSubtitleSettingsRepository(file).style.first())
        } finally {
            file.delete()
        }
    }
}
