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

    @Test fun uriParses() {
        val uri = android.net.Uri.parse("https://example.com/a/b.jpg?x=1")
        assertEquals("example.com", uri.host)
        assertEquals("b.jpg", uri.lastPathSegment)
        assertEquals("1", uri.getQueryParameter("x"))
    }
}
