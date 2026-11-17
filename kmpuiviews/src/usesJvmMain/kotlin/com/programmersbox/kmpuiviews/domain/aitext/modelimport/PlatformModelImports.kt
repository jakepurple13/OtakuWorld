package com.programmersbox.kmpuiviews.domain.aitext.modelimport

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.flow.StateFlow

/** [ModelImports] on Android and desktop: FileKit files into [importer], the current file from [localModel]. */
class PlatformModelImports(
    private val importer: ModelImporter,
    private val localModel: LocalModel,
) : ModelImports {
    override val state: StateFlow<ImportState> = importer.state

    override fun import(file: PlatformFile, replacing: String?) =
        importer.import(file.name, file.size(), { file.source() }, replacing)

    override fun cancel() = importer.cancel()
    override fun dismissError() = importer.dismissError()
    override fun remove(fileName: String) = importer.remove(fileName)

    override suspend fun current(): ImportedModel? =
        localModel.currentFile()?.let { ImportedModel(it.name, it.length()) }
}
