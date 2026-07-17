package com.programmersbox.jsextensionloader

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.DataStoreSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.File

class JsExtensionUpdateSettingsTest {

    @BeforeTest
    fun setUp() {
        val tempFile = File.createTempFile("test-datastore", ".preferences_pb")
        tempFile.delete()
        tempFile.deleteOnExit()
        DataStoreSettings(producePath = { tempFile.absolutePath })
    }

    @Test
    fun defaultsToNotify() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling())
        assertEquals(ExtensionUpdateMode.NOTIFY, settings.getMode())
    }

    @Test
    fun setModePersistsAndGetModeReadsItBack() = runTest {
        val settings = JsExtensionUpdateSettings(DataStoreHandling())
        settings.setMode(ExtensionUpdateMode.AUTOMATIC)
        assertEquals(ExtensionUpdateMode.AUTOMATIC, settings.getMode())
        settings.setMode(ExtensionUpdateMode.DISABLED)
        assertEquals(ExtensionUpdateMode.DISABLED, settings.getMode())
    }
}
