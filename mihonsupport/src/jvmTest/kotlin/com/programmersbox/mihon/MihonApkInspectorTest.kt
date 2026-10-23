package com.programmersbox.mihon

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MihonApkInspectorTest {
    private val fixture get() = MihonFixture.apk

    @Test
    fun readsFixtureManifest() {
        val info = MihonApkInspector.inspect(fixture)
        assertTrue(info.isExtension)
        assertEquals(MihonFixture.PACKAGE, info.packageName)
        assertEquals(MihonFixture.VERSION_NAME, info.versionName)
        assertEquals(MihonFixture.VERSION_CODE, info.versionCode)
        assertTrue(MihonLib.isSupported(info.libVersion))
        assertEquals(listOf(MihonFixture.SOURCE_CLASS), info.sourceClasses)
        assertTrue(info.label.isNotBlank() && !info.label.startsWith("Tachiyomi: "))
    }

    @Test
    fun fixtureSignedByRepoKey() {
        assertEquals(MihonFixture.REPO_FINGERPRINT, MihonApkInspector.inspect(fixture).fingerprints.last())
    }

    @Test
    fun tamperedApkHasNoFingerprints() {
        val data = fixture.readBytes()
        val target = classesDexDataOffset(data) + 16
        data[target] = (data[target].toInt() xor 0xFF).toByte()
        val tampered = File.createTempFile("tampered", ".apk").apply { deleteOnExit() }
        tampered.writeBytes(data)
        assertEquals(emptyList(), MihonApkInspector.inspect(tampered).fingerprints)
    }

    /** Offset of the first byte of the classes.dex entry data, found via its zip local file header. */
    private fun classesDexDataOffset(data: ByteArray): Int {
        val name = "classes.dex".toByteArray()
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until data.size - 30) {
            if (buf.getInt(i) != 0x04034b50) continue
            val nameLen = buf.getShort(i + 26).toInt()
            val extraLen = buf.getShort(i + 28).toInt()
            if (nameLen == name.size && data.copyOfRange(i + 30, i + 30 + nameLen).contentEquals(name)) {
                return i + 30 + nameLen + extraLen
            }
        }
        error("classes.dex not found")
    }
}
