package com.programmersbox.novel_sources

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.programmersbox.novel_sources.novels.WuxiaWorld
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.programmersbox.novel_sources.test", appContext.packageName)
    }

    @Test
    fun wuxiaworldTest() {
        val b = WuxiaWorld.baseUrl
        val url = "$b/api/novels/search"

        val j = postApi(url)

        println(j)
    }
}