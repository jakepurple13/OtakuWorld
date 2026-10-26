package com.programmersbox.aniyomi

import com.programmersbox.kmpextensionloader.ApkInstallException
import com.programmersbox.kmpextensionloader.ApkInstallFailure
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AniyomiApkStoreTest {
    private val root = Files.createTempDirectory("aniyomistore").toFile()
    private val apkDir = File(root, "apks")
    private val jarDir = File(root, "jars")

    @AfterTest fun cleanup() {
        root.deleteRecursively()
    }

    private fun tempCopy(src: File = AniyomiFixture.apk): File =
        File(root, "dl-${System.nanoTime()}.apk").also { src.copyTo(it) }

    private fun store() = AniyomiApkStore(apkDir, jarDir)

    private val fixtureInfo = AniyomiApkInfo(
        packageName = AniyomiFixture.PACKAGE,
        versionName = AniyomiFixture.VERSION_NAME,
        versionCode = AniyomiFixture.VERSION_CODE,
        label = "AnimeOnsen",
        isExtension = true,
        hasMetadata = true,
        libVersion = 14.0,
        sourceClasses = AniyomiFixture.SOURCE_CLASS,
        nsfw = false,
        fingerprints = emptyList(),
    )

    @Test fun acceptsAnimeApkOnly() {
        val store = store()
        assertTrue(store.accepts(AniyomiFixture.apk))
        assertFalse(store.accepts(AniyomiFixture.mangaApk))
        assertFalse(store.accepts(File(root, "junk.apk").also { it.writeText("not a zip") }))
        assertFalse(store.accepts(File(root, "missing.apk")))
    }

    @Test fun installStoresApkAndJar() = runTest {
        val store = store()
        assertEquals(AniyomiFixture.PACKAGE, store.install(tempCopy()))
        val installed = store.installed().single()
        assertTrue(installed.apk.exists())
        assertTrue(installed.jar!!.exists())
        assertEquals(AniyomiFixture.PACKAGE, installed.info.packageName)
    }

    @Test fun rejectsMangaApk() = runTest {
        val e = assertFailsWith<ApkInstallException> { store().install(tempCopy(AniyomiFixture.mangaApk)) }
        assertEquals(ApkInstallFailure.INVALID, e.failure)
        assertTrue(apkDir.listFiles().isNullOrEmpty())
    }

    @Test fun rejectsUnsupportedLib() = runTest {
        val store = AniyomiApkStore(apkDir, jarDir, inspect = { fixtureInfo.copy(libVersion = 13.0) })
        val e = assertFailsWith<ApkInstallException> { store.install(tempCopy()) }
        assertEquals(ApkInstallFailure.INCOMPATIBLE, e.failure)
        assertTrue(apkDir.listFiles().isNullOrEmpty())
    }

    @Test fun rejectsPathTraversalPackage() = runTest {
        val store = AniyomiApkStore(apkDir, jarDir, inspect = { fixtureInfo.copy(packageName = "../evil") })
        val e = assertFailsWith<ApkInstallException> { store.install(tempCopy()) }
        assertEquals(ApkInstallFailure.INVALID, e.failure)
        assertTrue(apkDir.listFiles().isNullOrEmpty())
        assertFalse(File(root, "evil.apk").exists())
        assertFalse(store.uninstall("../evil"))
        assertNull(store.versionName("../evil"))
    }

    @Test fun uninstallDeletesApkAndJar() = runTest {
        val store = store()
        store.install(tempCopy())
        val jar = store.installed().single().jar!!
        assertTrue(store.uninstall(AniyomiFixture.PACKAGE))
        assertTrue(store.installed().isEmpty())
        assertNull(store.versionName(AniyomiFixture.PACKAGE))
        assertFalse(jar.exists())
        assertFalse(store.uninstall(AniyomiFixture.PACKAGE))
    }

    @Test fun versionNameFromManifest() = runTest {
        val store = store()
        assertNull(store.versionName(AniyomiFixture.PACKAGE))
        store.install(tempCopy())
        val noInspect = AniyomiApkStore(apkDir, jarDir, inspect = { error("inspect must not run") })
        assertEquals(AniyomiFixture.VERSION_NAME, noInspect.versionName(AniyomiFixture.PACKAGE))
    }
}
