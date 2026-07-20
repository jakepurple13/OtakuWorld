package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs

class JSExtensionLoader(private val hostBridge: HostBridge) {

    fun load(scriptText: String, fileName: String, companionManifestJson: String?): JsExtension {
        val sourceId = fileName.substringBeforeLast(".")
        val manifest = ExtensionManifestParser.parse(scriptText, companionManifestJson, sourceId)
        val isTypeScript = fileName.endsWith(".ts")
        val transpiled = if (isTypeScript) TsTranspiler.transpile(scriptText) else scriptText

        val quickJs = QuickJs.create()
        // Default is 512KB. fetchAndParse splices whole fetched response bodies into
        // compiled JS source as string literals, and on-device reports show that
        // overflowing QuickJs's own stack budget during compile of a large real page.
        quickJs.maxStackSize = 4L * 1024 * 1024
        quickJs.evaluate(transpiled, fileName)

        val missing = ExtensionValidator.validate(quickJs)
        if (missing.isNotEmpty()) {
            quickJs.close()
            throw ExtensionValidationException(missing)
        }

        return JsExtension(manifest, quickJs, hostBridge)
    }
}
