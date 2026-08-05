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

    @Test
    fun `missing Composable annotation produces the exact expected error`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Sample", description = "desc", group = "Group")
            fun NotComposable() {}
            """.trimIndent(),
        )

        val result = compile(source)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "Function 'NotComposable' is annotated with @ShowcaseComponent but is not a @Composable function"
            )
        )
    }

    @Test
    fun `function with parameters produces the exact expected error`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Sample", description = "desc", group = "Group")
            @Composable
            fun WithParams(text: String) {}
            """.trimIndent(),
        )

        val result = compile(source)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "Function 'WithParams' is annotated with @ShowcaseComponent but has parameters. Showcase components must have zero parameters."
            )
        )
    }

    @Test
    fun `entries across multiple groups are sorted by group then name regardless of declaration order`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Zeta", description = "z", group = "Widgets")
            @Composable
            fun ZetaWidget() {}

            @ShowcaseComponent(name = "Alpha", description = "a", group = "Widgets")
            @Composable
            fun AlphaWidget() {}

            @ShowcaseComponent(name = "Only", description = "c", group = "Cards")
            @Composable
            fun CardSample() {}
            """.trimIndent(),
        )

        val generated = generatedRegistrySource(source)

        val cardIndex = generated.indexOf("name = \"Only\"")
        val alphaIndex = generated.indexOf("name = \"Alpha\"")
        val zetaIndex = generated.indexOf("name = \"Zeta\"")

        assertTrue(cardIndex in 0 until alphaIndex, "Cards group ('Only') must come before Widgets group entries")
        assertTrue(alphaIndex in 0 until zetaIndex, "Alpha must come before Zeta within the Widgets group")
    }
}
