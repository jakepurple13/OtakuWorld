package com.programmersbox.kmpuiviews.presentation.debug.navmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** System property the desktop `run` tasks set to the generated graph; packaged apps never set it. */
const val NAV_GRAPH_PROPERTY = "otaku.navgraph"

/** Desktop graph source: reads the file at [path], which is only set for local Gradle runs. */
class FileNavGraphSource(
    private val path: () -> String? = { System.getProperty(NAV_GRAPH_PROPERTY) },
) : NavGraphSource {

    private fun file(): File? = path()?.let(::File)?.takeIf { it.isFile }

    override val available: Boolean get() = file() != null

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        runCatching { file()?.readText() }.getOrNull()
    }
}
