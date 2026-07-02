package com.programmersbox.kmpuiviews.repository

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.programmersbox.kmpmodels.ExampleService
import com.programmersbox.kmpmodels.KmpSourceInformation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceInfoRepositoryTest {

    // Minimal fake: the JVM actual's copyUrl is a no-op, so this just needs to satisfy the
    // Clipboard interface without requiring a Composable/UI context to construct.
    private class FakeClipboard : Clipboard {
        override suspend fun getClipEntry(): ClipEntry? = null
        override suspend fun setClipEntry(clipEntry: ClipEntry?) {}

        @Suppress("OVERRIDE_DEPRECATION")
        override val nativeClipboard: Any = Any()
    }

    private fun sourceInfo(name: String, packageName: String = name) = KmpSourceInformation(
        apiService = ExampleService(),
        name = name,
        icon = null,
        packageName = packageName,
    )

    private val repository = SourceInfoRepository()

    @Test fun `versionName returns the source's name`() {
        val info = sourceInfo("Example Source")

        assertEquals("Example Source", repository.versionName(info))
    }

    @Test fun `uninstall does nothing and does not throw on JVM`() {
        val info = sourceInfo("Example Source")

        repository.uninstall(info)
    }

    @Test fun `copyUrl does nothing and does not throw on JVM`() = runTest {
        repository.copyUrl(FakeClipboard(), "https://example.com")
    }
}
