package com.programmersbox.jsextensionloader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class JsExtensionRepository {

    private val _extensions = MutableStateFlow<List<JsExtension>>(emptyList())
    val extensions: StateFlow<List<JsExtension>> = _extensions

    fun register(extension: JsExtension) {
        var replaced: JsExtension? = null
        _extensions.update { current ->
            replaced = current.find { it.manifest.id == extension.manifest.id }
            current.filterNot { it.manifest.id == extension.manifest.id } + extension
        }
        replaced?.close()
    }

    fun unload(id: String) {
        var toClose: JsExtension? = null
        _extensions.update { current ->
            toClose = current.find { it.manifest.id == id }
            current.filterNot { it.manifest.id == id }
        }
        toClose?.close()
    }
}
