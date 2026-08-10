package com.programmersbox.showcase.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ShowcaseSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val moduleId = environment.options["showcaseModuleId"]
        if (moduleId.isNullOrBlank()) {
            environment.logger.error(
                "The showcase processor requires a 'showcaseModuleId' KSP argument. " +
                    "Add `ksp { arg(\"showcaseModuleId\", \"<unique-module-name>\") }` " +
                    "to this module's build.gradle.kts."
            )
        }
        return ShowcaseSymbolProcessor(environment.codeGenerator, environment.logger, moduleId.orEmpty())
    }
}
