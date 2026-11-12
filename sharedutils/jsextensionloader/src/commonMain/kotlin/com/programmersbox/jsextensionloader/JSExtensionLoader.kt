package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/** Caps one extension's QuickJs heap. */
private const val JS_MEMORY_LIMIT = 128L * 1024 * 1024

/** Longest a single evaluate() may run before QuickJs interrupts it. */
private val JS_EVAL_TIMEOUT = 30.seconds

class JSExtensionLoader(private val hostBridge: HostBridge) {

    suspend fun load(scriptText: String, fileName: String, companionManifestJson: String?): JsExtension {
        val sourceId = fileName.substringBeforeLast(".")
        val manifest = ExtensionManifestParser.parse(scriptText, companionManifestJson, sourceId)
        val isTypeScript = fileName.endsWith(".ts")
        val transpiled = if (isTypeScript) TsTranspiler.transpile(scriptText) else scriptText

        val dispatcher = singleThreadQuickJsDispatcher("quickjs-$sourceId")
        return try {
            withContext(dispatcher) {
                val quickJs = QuickJs.create()
                try {
                    val deadline = JsEvalDeadline(JS_EVAL_TIMEOUT)
                    quickJs.memoryLimit = JS_MEMORY_LIMIT
                    quickJs.interruptHandler = deadline
                    deadline.guard { quickJs.evaluate(transpiled, fileName) }

                    val missing = deadline.guard { ExtensionValidator.validate(quickJs) }
                    if (missing.isNotEmpty()) throw ExtensionValidationException(missing)

                    JsExtension(manifest, quickJs, hostBridge, dispatcher, deadline)
                } catch (t: Throwable) {
                    quickJs.close()
                    throw t
                }
            }
        } catch (t: Throwable) {
            // Script and validation errors must not leak the thread.
            closeQuickJsDispatcher(dispatcher)
            throw t
        }
    }
}
