package com.programmersbox.kmpextensionloader

import org.objectweb.asm.ClassReader
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionRefScannerTest {

    // Scanned from its compiled bytes on the test classpath.
    @Suppress("unused")
    private class Sample : Runnable {
        private val text: StringBuilder = StringBuilder()

        override fun run() {
            text.append("x")
            try {
                java.util.UUID.randomUUID()
            } catch (e: IllegalStateException) {
                throw e
            }
        }
    }

    private val host = ExtensionRefScannerTest::class.java.classLoader

    @Test fun collectsSupertypesFieldTypesCallsAndCatchTypes() {
        val bytes = Sample::class.java.getResourceAsStream("ExtensionRefScannerTest\$Sample.class")!!.use { it.readBytes() }
        val refs = ExtensionRefScanner.references(ClassReader(bytes))

        assertContains(refs, "java/lang/Runnable")
        assertContains(refs, "java/lang/StringBuilder")
        assertContains(refs, "java/lang/IllegalStateException")
        assertContains(refs, "java/lang/StringBuilder append (Ljava/lang/String;)Ljava/lang/StringBuilder;")
        assertContains(refs, "java/util/UUID randomUUID ()Ljava/util/UUID;")
    }

    @Test fun resolvesTypesAndMembersThroughTheHierarchy() {
        assertTrue(ExtensionRefScanner.resolves("java/util/ArrayList", host))
        // Declared on AbstractCollection/Object, reached through supertypes.
        assertTrue(ExtensionRefScanner.resolves("java/util/ArrayList toString ()Ljava/lang/String;", host))
        assertTrue(ExtensionRefScanner.resolves("java/util/ArrayList <init> ()V", host))
        assertFalse(ExtensionRefScanner.resolves("java/util/ArrayList noSuchMethod ()V", host))
        assertFalse(ExtensionRefScanner.resolves("android/does/not/Exist", host))
    }

    @Test fun shapeIndexWithoutJarsSaysUnknown() {
        assertEquals("unknown (not in shape jars)", ExtensionRefScanner.ShapeIndex(emptyList()).describe("android/app/Dialog"))
    }
}
