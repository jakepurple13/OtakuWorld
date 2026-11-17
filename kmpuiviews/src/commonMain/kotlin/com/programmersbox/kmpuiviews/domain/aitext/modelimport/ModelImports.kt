package com.programmersbox.kmpuiviews.domain.aitext.modelimport

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.StateFlow

sealed interface ImportState {
    data object Idle : ImportState
    data class Copying(val name: String, val copied: Long, val total: Long) : ImportState
    data class Checking(val name: String) : ImportState
    data class Failed(val reason: ImportError) : ImportState
}

sealed interface ImportError {
    data object NotAModel : ImportError
    data class NoSpace(val needed: Long, val available: Long) : ImportError
    data class WontLoad(val message: String?) : ImportError
    data class CopyFailed(val message: String?) : ImportError
}

/** The model file the local engine runs. */
data class ImportedModel(val fileName: String, val sizeBytes: Long)

/** Importing a model file into the app, for the AI page. Not bound where there is no local engine (iOS). */
interface ModelImports {
    val state: StateFlow<ImportState>

    /** Imports [file]; [replacing] is deleted once the new file passes its check. */
    fun import(file: PlatformFile, replacing: String?)
    fun cancel()
    fun dismissError()
    fun remove(fileName: String)

    /** The model file the local engine runs now, or null. */
    suspend fun current(): ImportedModel?
}
