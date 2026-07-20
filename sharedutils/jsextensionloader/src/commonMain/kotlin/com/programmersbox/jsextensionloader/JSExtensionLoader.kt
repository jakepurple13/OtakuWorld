package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import kotlinx.coroutines.withContext

class JSExtensionLoader(private val hostBridge: HostBridge) {

    suspend fun load(scriptText: String, fileName: String, companionManifestJson: String?): JsExtension {
        val sourceId = fileName.substringBeforeLast(".")
        val manifest = ExtensionManifestParser.parse(scriptText, companionManifestJson, sourceId)
        val isTypeScript = fileName.endsWith(".ts")
        val transpiled = if (isTypeScript) TsTranspiler.transpile(scriptText) else scriptText

        val dispatcher = singleThreadQuickJsDispatcher("quickjs-$sourceId")
        return withContext(dispatcher) {
            val quickJs = QuickJs.create()
            quickJs.evaluate(transpiled, fileName)

            val missing = ExtensionValidator.validate(quickJs)
            if (missing.isNotEmpty()) {
                quickJs.close()
                closeQuickJsDispatcher(dispatcher)
                throw ExtensionValidationException(missing)
            }

            JsExtension(manifest, quickJs, hostBridge, dispatcher)
        }
    }
}
