package com.programmersbox.jsextensionloader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class JsExtensionRepository {

    private val _extensions = MutableStateFlow<List<JsExtension>>(emptyList())
    val extensions: StateFlow<List<JsExtension>> = _extensions

    fun register(extension: JsExtension) {
        _extensions.update { current ->
            current.filterNot { it.manifest.id == extension.manifest.id } + extension
        }
    }

    fun unload(id: String) {
        _extensions.update { current ->
            current.find { it.manifest.id == id }?.close()
            current.filterNot { it.manifest.id == id }
        }
    }
}
