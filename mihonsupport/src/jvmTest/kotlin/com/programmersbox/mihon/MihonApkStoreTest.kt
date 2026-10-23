package com.programmersbox.mihon

import com.programmersbox.kmpextensionloader.ApkInstallException
import com.programmersbox.kmpextensionloader.ApkInstallFailure
import com.programmersbox.kmpextensionloader.DexConverter
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MihonApkStoreTest {
    private val root = Files.createTempDirectory("apkstore").toFile()
    private val apkDir = File(root, "apks")
    private val jarDir = File(root, "jars")

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    private fun tempFixture(): File = File(root, "dl-${System.nanoTime()}.apk").also { MihonFixture.apk.copyTo(it) }

    private fun store() = MihonApkStore(apkDir, jarDir)

    private val fixtureInfo = MihonApkInfo(
        packageName = MihonFixture.PACKAGE,
        versionName = MihonFixture.VERSION_NAME,
        versionCode = MihonFixture.VERSION_CODE,
        label = "Mangapill",
        isExtension = true,
        hasMetadata = true,
        libVersion = 1.6,
        sourceClasses = listOf(MihonFixture.SOURCE_CLASS),
        nsfw = false,
        fingerprints = emptyList(),
    )

    @Test fun installStoresApkAndJar() = runTest {
        val store = store()
        assertEquals(MihonFixture.PACKAGE, store.install(tempFixture()))
        val installed = store.installed()
        assertEquals(1, installed.size)
        assertTrue(installed.single().jar!!.exists())
        assertEquals(MihonFixture.VERSION_NAME, store.versionName(MihonFixture.PACKAGE))
    }

    @Test fun installRejectsNonExtension() = runTest {
        val fake = File(root, "plain.apk")
        ZipOutputStream(fake.outputStream()).use {
            it.putNextEntry(ZipEntry("a.txt"))
            it.write(1)
            it.closeEntry()
        }
        val e = assertFailsWith<ApkInstallException> { store().install(fake) }
        assertEquals(ApkInstallFailure.INVALID, e.failure)
        assertTrue(apkDir.listFiles().isNullOrEmpty())
    }

    @Test fun installRejectsUnsupportedLib() = runTest {
        val store = MihonApkStore(apkDir, jarDir, inspect = { fixtureInfo.copy(libVersion = 1.2) })
        val e = assertFailsWith<ApkInstallException> { store.install(tempFixture()) }
        assertEquals(ApkInstallFailure.INCOMPATIBLE, e.failure)
        assertTrue(apkDir.listFiles().isNullOrEmpty())
    }

    @Test fun uninstallDeletesApkAndJar() = runTest {
        val store = store()
        store.install(tempFixture())
        val jar = store.installed().single().jar!!
        assertTrue(store.uninstall(MihonFixture.PACKAGE))
        assertTrue(store.installed().isEmpty())
        assertNull(store.versionName(MihonFixture.PACKAGE))
        assertFalse(jar.exists())
        assertFalse(store.uninstall(MihonFixture.PACKAGE))
    }

    @Test fun installReplacesApkAndKeepsOldJarUntilCleanup() = runTest {
        var version = "1"
        val store = MihonApkStore(
            apkDir, jarDir,
            inspect = { fixtureInfo.copy(versionName = version) },
            convert = { apk, dir -> DexConverter.jarFor(apk, dir).also { dir.mkdirs(); it.writeText("jar") } },
        )
        store.install(tempFixture())
        val oldJar = store.installed().single().jar!!
        version = "2"
        store.install(tempFixture().also { it.appendBytes(byteArrayOf(0)) })
        val newJar = store.installed().single().jar!!
        assertEquals(1, apkDir.listFiles()!!.size)
        // versionName reads the manifest, not the inspect seam.
        assertEquals(MihonFixture.VERSION_NAME, store.versionName(MihonFixture.PACKAGE))
        assertTrue(oldJar.exists() && newJar.exists() && oldJar != newJar)
        store.cleanupStaleJars()
        assertFalse(oldJar.exists())
        assertTrue(newJar.exists())
    }

    @Test fun installRejectsTraversalPackageName() = runTest {
        val store = MihonApkStore(apkDir, jarDir, inspect = { fixtureInfo.copy(packageName = "../evil") })
        val e = assertFailsWith<ApkInstallException> { store.install(tempFixture()) }
        assertEquals(ApkInstallFailure.INVALID, e.failure)
        assertTrue(apkDir.listFiles().isNullOrEmpty())
        assertFalse(File(root, "evil.apk").exists())
    }

    @Test fun uninstallRejectsTraversalPackageName() = runTest {
        apkDir.mkdirs()
        val sentinel = File(root, "evil.apk").also { it.writeText("x") }
        assertFalse(store().uninstall("../evil"))
        assertTrue(sentinel.exists())
    }

    @Test fun versionNameRejectsTraversal() {
        apkDir.mkdirs()
        MihonFixture.apk.copyTo(File(root, "evil.apk"))
        assertNull(store().versionName("../evil"))
    }

    @Test fun installedListsPackageWhoseConversionFails() = runTest {
        store().install(tempFixture())
        val failing = MihonApkStore(apkDir, jarDir, convert = { _, _ -> null })
        val entry = failing.installed().single()
        assertEquals(MihonFixture.PACKAGE, entry.info.packageName)
        assertNull(entry.jar)
    }

    @Test fun cleanupKeepsInProgressConversions() {
        jarDir.mkdirs()
        val hash = "a".repeat(64)
        val inProgress = File(jarDir, "$hash.jar.part").also { it.writeText("partial") }
        val legacyTemp = File(jarDir, "$hash.tmp.jar").also { it.writeText("partial") }
        val stale = File(jarDir, "$hash-v1.jar").also { it.writeText("old") }
        store().cleanupStaleJars()
        assertTrue(inProgress.exists())
        assertTrue(legacyTemp.exists())
        assertFalse(stale.exists())
    }

    @Test fun conversionTempFileIsNotACachedJar() {
        val converted = File(root, "conv")
        assertTrue(DexConverter.convert(MihonFixture.apk, converted) != null)
        assertTrue(converted.listFiles()!!.all { DexConverter.isCachedJar(it) })
        assertFalse(DexConverter.isCachedJar(File(converted, "${"a".repeat(64)}.jar.part").also { it.writeText("x") }))
    }

    @Test fun versionNameSkipsFullInspection() = runTest {
        store().install(tempFixture())
        val noInspect = MihonApkStore(apkDir, jarDir, inspect = { error("inspect must not run") })
        assertEquals(MihonFixture.VERSION_NAME, noInspect.versionName(MihonFixture.PACKAGE))
    }
}
