package com.programmersbox.showcase.processor

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
class ShowcaseSymbolProcessorTest {

    private fun compile(source: SourceFile): JvmCompilationResult {
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            configureKsp {
                symbolProcessorProviders += ShowcaseSymbolProcessorProvider()
            }
            inheritClassPath = true
        }
        return compilation.compile()
    }

    private fun generatedRegistrySource(source: SourceFile): String {
        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            configureKsp {
                symbolProcessorProviders += ShowcaseSymbolProcessorProvider()
            }
            inheritClassPath = true
        }
        compilation.compile()
        return File(
            compilation.kspSourcesDir,
            "kotlin/com/programmersbox/showcase/generated/ShowcaseRegistry.kt",
        ).readText()
    }

    @Test
    fun `valid zero-arg composable processes successfully and extracts name, description, group`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Sample Button", description = "A sample button", group = "Buttons")
            @Composable
            fun SampleButton() {}
            """.trimIndent(),
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val generated = generatedRegistrySource(source)
        assertTrue(generated.contains("name = \"Sample Button\""))
        assertTrue(generated.contains("description = \"A sample button\""))
        assertTrue(generated.contains("group = \"Buttons\""))
        assertTrue(generated.contains("content = { test.SampleButton() }"))
    }
}
