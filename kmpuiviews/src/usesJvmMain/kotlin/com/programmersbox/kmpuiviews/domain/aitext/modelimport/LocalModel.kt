package com.programmersbox.kmpuiviews.domain.aitext.modelimport

import java.io.File

/** The on-device LiteRT engine, as the import flow sees it. */
interface LocalModel {
    /** The model file the engine runs now, or null when there is none. */
    suspend fun currentFile(): File?

    /** Loads [file] where it is and runs one short prompt. Throws when the model can't be used. */
    suspend fun verify(file: File)
}
