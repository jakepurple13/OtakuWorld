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

    private fun generatedRegistrySource(source: SourceFile): String =
        generatedRegistrySource(listOf(source))

    private fun generatedRegistrySource(sources: List<SourceFile>): String {
        val compilation = KotlinCompilation().apply {
            this.sources = sources
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
    fun `non top-level function produces the exact expected error`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            class Container {
                @ShowcaseComponent(name = "Sample", description = "desc", group = "Group")
                @Composable
                fun NestedComposable() {}
            }
            """.trimIndent(),
        )

        val result = compile(source)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "Function 'NestedComposable' is annotated with @ShowcaseComponent but is not a top-level function. Showcase components must be top-level."
            )
        )
    }

    @Test
    fun `private function produces the exact expected error`() {
        val source = SourceFile.kotlin(
            "Sample.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Sample", description = "desc", group = "Group")
            @Composable
            private fun PrivateComposable() {}
            """.trimIndent(),
        )

        val result = compile(source)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains(
                "Function 'PrivateComposable' is annotated with @ShowcaseComponent but is private. Showcase components must not be private."
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

    @Test
    fun `entries across multiple files and 3 or more groups are sorted by group then name`() {
        val fileOne = SourceFile.kotlin(
            "FileOne.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Zeta", description = "z", group = "Widgets")
            @Composable
            fun ZetaWidget() {}

            @ShowcaseComponent(name = "Only", description = "d", group = "Dialogs")
            @Composable
            fun DialogSample() {}
            """.trimIndent(),
        )

        val fileTwo = SourceFile.kotlin(
            "FileTwo.kt",
            """
            package test

            import androidx.compose.runtime.Composable
            import com.programmersbox.showcase.annotations.ShowcaseComponent

            @ShowcaseComponent(name = "Alpha", description = "a", group = "Widgets")
            @Composable
            fun AlphaWidget() {}

            @ShowcaseComponent(name = "Only", description = "c", group = "Cards")
            @Composable
            fun CardSample() {}
            """.trimIndent(),
        )

        val generated = generatedRegistrySource(listOf(fileOne, fileTwo))

        val cardIndex = generated.indexOf("content = { test.CardSample() }")
        val dialogIndex = generated.indexOf("content = { test.DialogSample() }")
        val alphaIndex = generated.indexOf("content = { test.AlphaWidget() }")
        val zetaIndex = generated.indexOf("content = { test.ZetaWidget() }")

        assertTrue(cardIndex in 0 until dialogIndex, "Cards group must come before Dialogs group")
        assertTrue(dialogIndex in 0 until alphaIndex, "Dialogs group must come before Widgets group")
        assertTrue(alphaIndex in 0 until zetaIndex, "Alpha must come before Zeta within the Widgets group")
    }
}
