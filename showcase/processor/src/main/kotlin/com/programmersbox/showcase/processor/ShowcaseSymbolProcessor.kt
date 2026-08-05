package com.programmersbox.showcase.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Visibility

private const val SHOWCASE_ANNOTATION = "com.programmersbox.showcase.annotations.ShowcaseComponent"
private const val COMPOSABLE_ANNOTATION = "androidx.compose.runtime.Composable"
private const val PROVIDER_INTERFACE = "com.programmersbox.showcase.annotations.ShowcaseRegistryProvider"
private const val GENERATED_PACKAGE = "com.programmersbox.showcase.generated"

private data class GeneratedEntry(
    val name: String,
    val description: String,
    val group: String,
    val qualifiedReference: String,
)

class ShowcaseSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val moduleId: String,
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

        if (moduleId.isBlank()) return emptyList()

        val functions = resolver.getSymbolsWithAnnotation(SHOWCASE_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        println("Function count: ${functions.size}")

        val entries = functions.mapNotNull { function -> toEntryOrReportError(function) }

        val sortedEntries = entries.sortedWith(compareBy({ it.group }, { it.name }, { it.qualifiedReference }))
        val dependencies = Dependencies(
            aggregating = true,
            *functions.mapNotNull { it.containingFile }.toTypedArray(),
        )

        val className = "${sanitizedModuleId()}ShowcaseRegistryProvider"
        val qualifiedClassName = "$GENERATED_PACKAGE.$className"

        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = GENERATED_PACKAGE,
            fileName = className,
        ).bufferedWriter().use { writer -> writer.write(generateFileContents(className, sortedEntries)) }

        codeGenerator.createNewFileByPath(
            dependencies = dependencies,
            path = "META-INF/services/$PROVIDER_INTERFACE",
            extensionName = "",
        ).bufferedWriter().use { writer -> writer.write(qualifiedClassName) }

        return emptyList()
    }

    private fun sanitizedModuleId(): String {
        val sanitized = moduleId.replace(Regex("[^A-Za-z0-9]"), "_")
        val safe = if (sanitized.firstOrNull()?.isDigit() == true) "_$sanitized" else sanitized
        return safe.replaceFirstChar { it.uppercase() }
    }

    private fun toEntryOrReportError(function: KSFunctionDeclaration): GeneratedEntry? {
        val functionName = function.simpleName.asString()

        if (function.parentDeclaration != null) {
            logger.error(
                "Function '$functionName' is annotated with @ShowcaseComponent but is not a top-level function. Showcase components must be top-level.",
                function,
            )
            return null
        }

        if (function.getVisibility() == Visibility.PRIVATE) {
            logger.error(
                "Function '$functionName' is annotated with @ShowcaseComponent but is private. Showcase components must not be private.",
                function,
            )
            return null
        }

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

    private fun generateFileContents(className: String, entries: List<GeneratedEntry>): String = buildString {
        appendLine("package $GENERATED_PACKAGE")
        appendLine()
        appendLine("import com.programmersbox.showcase.annotations.ShowcaseEntry")
        appendLine("import com.programmersbox.showcase.annotations.ShowcaseRegistryProvider")
        appendLine()
        appendLine("class $className : ShowcaseRegistryProvider {")
        appendLine("    override val entries: List<ShowcaseEntry> = listOf(")
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
