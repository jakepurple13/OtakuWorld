package com.programmersbox.kmpextensionloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidStubShapeTest {
    @Test fun sharedPreferencesIsInterface() {
        assertTrue(android.content.SharedPreferences::class.java.isInterface)
        assertTrue(android.content.SharedPreferences.Editor::class.java.isInterface)
    }

    @Test fun preferenceHierarchyMatchesAndroidx() {
        assertEquals<Class<*>?>(androidx.preference.DialogPreference::class.java, androidx.preference.ListPreference::class.java.superclass)
        assertEquals<Class<*>?>(androidx.preference.TwoStatePreference::class.java, androidx.preference.SwitchPreferenceCompat::class.java.superclass)
        assertEquals<Class<*>?>(androidx.preference.PreferenceGroup::class.java, androidx.preference.PreferenceScreen::class.java.superclass)
        assertEquals<Class<*>?>(androidx.preference.Preference::class.java, androidx.preference.PreferenceGroup::class.java.superclass)
    }

    @Test fun packageInfoHasLongVersionCodeAccessors() {
        val cls = android.content.pm.PackageInfo::class.java
        assertEquals(Long::class.javaPrimitiveType, cls.getMethod("getLongVersionCode").returnType)
        cls.getMethod("setLongVersionCode", Long::class.javaPrimitiveType)
    }

    @Test fun uriParses() {
        val uri = android.net.Uri.parse("https://example.com/a/b.jpg?x=1")
        assertEquals("example.com", uri.host)
        assertEquals("b.jpg", uri.lastPathSegment)
        assertEquals("1", uri.getQueryParameter("x"))
    }

    @Test fun uriParseNeverThrowsOnIllegalChars() {
        val raw = "https://example.com/a b/c|d.jpg?q=x y"
        val uri = android.net.Uri.parse(raw)
        assertEquals("example.com", uri.host)
        assertEquals("c|d.jpg", uri.lastPathSegment)
        assertEquals("x y", uri.getQueryParameter("q"))
        assertEquals(raw, uri.toString())
    }

    @Test fun uriUnderscoreHost() {
        assertEquals("my_host.example.com", android.net.Uri.parse("https://my_host.example.com/p").host)
    }

    @Test fun uriWithoutPath() {
        val uri = android.net.Uri.parse("https://example.com")
        assertTrue(uri.path.isNullOrEmpty())
        assertEquals(null, uri.lastPathSegment)
    }
}
