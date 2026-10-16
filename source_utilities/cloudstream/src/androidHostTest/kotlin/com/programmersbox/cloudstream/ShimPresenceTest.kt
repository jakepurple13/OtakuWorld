package com.programmersbox.cloudstream

import android.app.Application
import android.content.SharedPreferences
import com.lagradost.cloudstream3.CloudStreamApp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ShimPresenceTest {

    // Plugins bind to these by name at load time; a missing class means a failed plugin.
    @Test
    fun allReferencedTypesResolve() {
        val rows = File("shims.md").readLines()
            .mapNotNull { ROW.find(it) }
            .map { it.groupValues[1] to it.groupValues[2] }
            .filterNot { (_, status) -> status == "missing" || status.startsWith("app ") }
        assertTrue(rows.size > 20, "shims.md table not parsed: ${rows.size} rows")

        val unresolved = rows.map { it.first }.filter { name ->
            runCatching { Class.forName(name, false, javaClass.classLoader) }.isFailure
        }
        if (unresolved.isNotEmpty()) fail("Unresolved types: $unresolved")
    }

    @Test
    fun setGetKeyRoundTrip() {
        val app = FakeApplication()
        CloudstreamShims.install(app)

        CloudStreamApp.setKey("folder/answer", Stored("x", 42))
        assertEquals(Stored("x", 42), CloudStreamApp.getKey<Stored>("folder/answer", null))
        assertEquals(listOf("folder/answer"), CloudStreamApp.getKeys("folder"))
        assertTrue(app.prefs.getValue("cloudstream_data").contains("folder/answer"))

        CloudStreamApp.removeKey("folder/answer")
        assertNull(CloudStreamApp.getKey<Stored>("folder/answer"))
        assertEquals(Stored("d", 0), CloudStreamApp.getKey("missing", Stored("d", 0)))
    }

    data class Stored(val name: String, val value: Int)

    private class FakeApplication : Application() {
        val prefs = mutableMapOf<String, FakePrefs>()
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            prefs.getOrPut(name) { FakePrefs() }

        override fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks?) = Unit
    }

    private class FakePrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = values.toMap()
        override fun getString(key: String, defValue: String?) = values[key] as String? ?: defValue
        override fun getStringSet(key: String, defValues: Set<String>?) =
            @Suppress("UNCHECKED_CAST") (values[key] as Set<String>? ?: defValues)
        override fun getInt(key: String, defValue: Int) = values[key] as Int? ?: defValue
        override fun getLong(key: String, defValue: Long) = values[key] as Long? ?: defValue
        override fun getFloat(key: String, defValue: Float) = values[key] as Float? ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) = values[key] as Boolean? ?: defValue
        override fun contains(key: String) = key in values
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var clear = false

            override fun putString(key: String, value: String?) = apply { pending[key] = value }
            override fun putStringSet(key: String, values: Set<String>?) = apply { pending[key] = values }
            override fun putInt(key: String, value: Int) = apply { pending[key] = value }
            override fun putLong(key: String, value: Long) = apply { pending[key] = value }
            override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
            override fun remove(key: String) = apply { removed += key }
            override fun clear() = apply { clear = true }
            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clear) values.clear()
                removed.forEach(values::remove)
                values.putAll(pending)
            }
        }
    }

    private companion object {
        val ROW = Regex("""^\| `([\w.$]+)` \|[^|]*\| ([^|]+?) \|$""")
    }
}
