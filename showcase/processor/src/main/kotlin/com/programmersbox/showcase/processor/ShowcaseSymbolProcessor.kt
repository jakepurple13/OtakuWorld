package com.programmersbox.showcase.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

private const val SHOWCASE_ANNOTATION = "com.programmersbox.showcase.annotations.ShowcaseComponent"
private const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"

private data class GeneratedEntry(
    val name: String,
    val description: String,
    val group: String,
    val qualifiedReference: String,
)

class ShowcaseSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // KSP re-invokes process() in a second round after this processor generates a new file
        // (the round loop continues as long as any processor produced a new source file). Since
        // the generated registry never contains new @ShowcaseComponent-annotated functions, and
        // this processor only needs a single pass over the user's sources, guard against
        // generating (and thus re-creating) the same output file on that second round.
        if (invoked) return emptyList()
        invoked = true

        val functions = resolver.getSymbolsWithAnnotation(SHOWCASE_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        val entries = functions.mapNotNull { function -> toEntryOrReportError(function) }

        val sortedEntries = entries.sortedWith(compareBy({ it.group }, { it.name }))
        val dependencies = Dependencies(
            aggregating = true,
            *functions.mapNotNull { it.containingFile }.toTypedArray(),
        )

        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = "com.programmersbox.showcase.generated",
            fileName = "ShowcaseRegistry",
        ).bufferedWriter().use { writer -> writer.write(generateFileContents(sortedEntries)) }

        return emptyList()
    }

    private fun toEntryOrReportError(function: KSFunctionDeclaration): GeneratedEntry? {
        val functionName = function.simpleName.asString()

        val isComposable = function.annotations.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == COMPOSABLE_ANNOTATION
        }
        if (!isComposable) {
            logger.error(
                "Function '$functionName' is annotated with @ShowcaseComponent but is not a @Composable function",
                function,
            )
            return null
        }

        if (function.parameters.isNotEmpty()) {
            logger.error(
                "Function '$functionName' is annotated with @ShowcaseComponent but has parameters. Showcase components must have zero parameters.",
                function,
            )
            return null
        }

        val annotation = function.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == SHOWCASE_ANNOTATION
        }
        val args = annotation.arguments.associateBy { it.name?.asString() }
        val name = args["name"]?.value as? String ?: ""
        val description = args["description"]?.value as? String ?: ""
        val group = args["group"]?.value as? String ?: ""

        val packageName = function.packageName.asString()
        val qualifiedReference = if (packageName.isEmpty()) functionName else "$packageName.$functionName"

        return GeneratedEntry(name, description, group, qualifiedReference)
    }

    private fun generateFileContents(entries: List<GeneratedEntry>): String = buildString {
        appendLine("package com.programmersbox.showcase.generated")
        appendLine()
        appendLine("import androidx.compose.runtime.Composable")
        appendLine()
        appendLine("data class ShowcaseEntry(")
        appendLine("    val name: String,")
        appendLine("    val description: String,")
        appendLine("    val group: String,")
        appendLine("    val content: @Composable () -> Unit,")
        appendLine(")")
        appendLine()
        appendLine("object ShowcaseRegistry {")
        appendLine("    val entries: List<ShowcaseEntry> = listOf(")
        entries.forEach { entry ->
            appendLine("        ShowcaseEntry(")
            appendLine("            name = ${entry.name.quoted()},")
            appendLine("            description = ${entry.description.quoted()},")
            appendLine("            group = ${entry.group.quoted()},")
            appendLine("            content = { ${entry.qualifiedReference}() },")
            appendLine("        ),")
        }
        appendLine("    )")
        appendLine("}")
    }

    private fun String.quoted(): String =
        "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
