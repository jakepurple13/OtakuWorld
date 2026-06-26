package com.programmersbox.kmpuiviews.presentation.components.custombackdrop

import org.intellij.lang.annotations.Language

sealed interface BackdropRuntimeShaderCache {

    fun obtainRuntimeShader(key: String, @Language("AGSL") string: String): BackdropRuntimeShader
}

internal class BackdropRuntimeShaderCacheImpl : BackdropRuntimeShaderCache {

    private val runtimeShaders = mutableMapOf<String, BackdropRuntimeShader>()

    override fun obtainRuntimeShader(key: String, string: String): BackdropRuntimeShader {
        return runtimeShaders.getOrPut(key) { BackdropRuntimeShader(string) }
    }

    fun clear() {
        runtimeShaders.clear()
    }
}