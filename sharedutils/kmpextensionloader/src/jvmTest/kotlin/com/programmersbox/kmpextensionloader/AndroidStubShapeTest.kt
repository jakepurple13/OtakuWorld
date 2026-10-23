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

    // Extension bytecode calls Application methods through ContextWrapper (Android: Application extends ContextWrapper).
    @Test fun applicationExtendsContextWrapperAndResolvesItsMethods() {
        assertEquals<Class<*>?>(android.content.ContextWrapper::class.java, android.app.Application::class.java.superclass)
        assertEquals<Class<*>?>(android.content.Context::class.java, android.content.ContextWrapper::class.java.superclass)
        val wrapper = android.content.ContextWrapper::class.java
        wrapper.getMethod("getSharedPreferences", String::class.java, Int::class.javaPrimitiveType)
        wrapper.getMethod("getCacheDir")
        wrapper.getMethod("getPackageName")
    }

    @Test fun preferenceManagerDefaultPreferences() {
        val dir = java.nio.file.Files.createTempDirectory("prefmgr").toFile()
        try {
            val app = android.app.Application("pkg.test", dir)
            val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(app)
            assertTrue(prefs === app.getSharedPreferences("pkg.test_preferences", 0))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test fun base64EncodeMatchesAndroidFlags() {
        val b64 = android.util.Base64
        val hello = "hello".toByteArray()
        assertEquals("aGVsbG8=\n", b64.encodeToString(hello, b64.DEFAULT))
        assertEquals("aGVsbG8=", b64.encodeToString(hello, b64.NO_WRAP))
        assertEquals("aGVsbG8\n", b64.encodeToString(hello, b64.NO_PADDING))
        assertEquals("aGVsbG8=\r\n", b64.encodeToString(hello, b64.CRLF))
        assertEquals("", b64.encodeToString(ByteArray(0), b64.DEFAULT))
        val urlBytes = byteArrayOf(-5, -1, -2)
        assertEquals("-__-", b64.encodeToString(urlBytes, b64.URL_SAFE or b64.NO_WRAP))
        assertEquals("+//+", b64.encodeToString(urlBytes, b64.NO_WRAP))
        assertEquals("ell", String(b64.decode(b64.encode("hello".toByteArray(), 1, 3, b64.NO_WRAP), b64.DEFAULT)))
        assertEquals("ZWxs", b64.encodeToString("hello".toByteArray(), 1, 3, b64.NO_WRAP or b64.NO_CLOSE))
    }

    @Test fun base64WrapsEvery76Characters() {
        val encoded = android.util.Base64.encodeToString(ByteArray(100) { it.toByte() }, android.util.Base64.DEFAULT)
        val lines = encoded.split('\n')
        assertEquals(listOf(76, 60, 0), lines.map { it.length })
    }

    @Test fun base64DecodeIsLenientLikeAndroid() {
        val b64 = android.util.Base64
        assertEquals("hello", String(b64.decode("aGVs\nbG8=\n", b64.DEFAULT)))
        assertEquals("hello", String(b64.decode("aGVsbG8", b64.DEFAULT)))
        assertTrue(byteArrayOf(-5, -1, -2).contentEquals(b64.decode("-__-", b64.URL_SAFE)))
        assertTrue(byteArrayOf(-5, -1, -2).contentEquals(b64.decode("+//+".toByteArray(), b64.DEFAULT)))
        kotlin.test.assertFailsWith<IllegalArgumentException> { b64.decode("-__-", b64.DEFAULT) }
        kotlin.test.assertFailsWith<IllegalArgumentException> { b64.decode("a\$bc", b64.DEFAULT) }
    }

    @Test fun orgJsonIsOnTheClasspath() {
        assertEquals(3, org.json.JSONObject("""{"a":[1,2,3]}""").getJSONArray("a").length())
    }

    @Test fun handlerPostsToTheMainLooperThread() {
        val main = android.os.Looper.getMainLooper()
        assertEquals(null, android.os.Looper.myLooper())
        val ran = java.util.concurrent.CompletableFuture<Pair<Boolean, android.os.Looper?>>()
        android.os.Handler(main).post { ran.complete(main.isCurrentThread() to android.os.Looper.myLooper()) }
        val (onMain, looper) = ran.get(5, java.util.concurrent.TimeUnit.SECONDS)
        assertTrue(onMain)
        assertTrue(looper === main)
        val delayed = java.util.concurrent.CompletableFuture<Unit>()
        android.os.Handler(main).postDelayed({ delayed.complete(Unit) }, 10)
        delayed.get(5, java.util.concurrent.TimeUnit.SECONDS)
    }

    @Test fun systemDisplayMetrics() {
        val metrics = android.content.res.Resources.getSystem().getDisplayMetrics()
        assertTrue(metrics.widthPixels > 0 && metrics.heightPixels > 0)
        android.util.DisplayMetrics::class.java.getField("widthPixels")
    }

    @Test fun lruCacheEvictsLeastRecentlyUsed() {
        val cache = android.util.LruCache<String, Int>(2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.get("a")
        cache.put("c", 3)
        assertEquals(null, cache.get("b"))
        assertEquals(1, cache.get("a"))
        assertEquals(3, cache.remove("c"))
        assertEquals(1, cache.size())
    }

    @Test fun uriEncodeMatchesAndroid() {
        assertEquals("a%20b%2Fc_-!.~'()*%C3%A9", android.net.Uri.encode("a b/c_-!.~'()*é"))
        assertEquals("a%20b/c", android.net.Uri.encode("a b/c", "/"))
        assertEquals(null, android.net.Uri.encode(null))
    }

    @Test fun logPrintlnAndBuildId() {
        assertEquals(0, android.util.Log.println(android.util.Log.INFO, "tag", "msg"))
        assertEquals("Desktop", android.os.Build.ID)
    }

    /** A two-tile descramble: swap the halves of a 2x1 image, then round-trip through PNG and JPEG. */
    @Test fun bitmapDescrambleRoundTrip() {
        val red = 0xFFFF0000.toInt()
        val blue = 0xFF0000FF.toInt()
        val scrambled = android.graphics.Bitmap.createBitmap(2, 1, android.graphics.Bitmap.Config.ARGB_8888)
        scrambled.setPixel(0, 0, red)
        scrambled.setPixel(1, 0, blue)
        val png = java.io.ByteArrayOutputStream()
        assertTrue(scrambled.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, png))

        val source = assertNotNullBitmap(android.graphics.BitmapFactory.decodeStream(java.io.ByteArrayInputStream(png.toByteArray())))
        val out = android.graphics.Bitmap.createBitmap(source.getWidth(), source.getHeight(), android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawBitmap(source, android.graphics.Rect(0, 0, 1, 1), android.graphics.Rect(1, 0, 2, 1), null)
        canvas.drawBitmap(source, android.graphics.Rect(1, 0, 2, 1), android.graphics.Rect(0, 0, 1, 1), android.graphics.Paint())
        assertEquals(blue, out.getPixel(0, 0))
        assertEquals(red, out.getPixel(1, 0))

        val webp = java.io.ByteArrayOutputStream()
        assertTrue(out.compress(android.graphics.Bitmap.CompressFormat.WEBP, 90, webp))
        assertEquals(blue, assertNotNullBitmap(android.graphics.BitmapFactory.decodeByteArray(webp.toByteArray(), 0, webp.size())).getPixel(0, 0))
        assertTrue(out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, java.io.ByteArrayOutputStream()))
        assertEquals(null, android.graphics.BitmapFactory.decodeStream(java.io.ByteArrayInputStream(byteArrayOf(1, 2, 3))))
    }

    @Test fun canvasOffsetDrawColorAndTransforms() {
        val src = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).apply { setPixel(0, 0, 0xFF00FF00.toInt()) }
        val out = android.graphics.Bitmap.createBitmap(3, 1, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(0xFFFFFFFF.toInt())
        canvas.save()
        canvas.translate(1f, 0f)
        canvas.drawBitmap(src, 1f, 0f, null)
        canvas.restore()
        canvas.drawBitmap(src, 0f, 0f, null)
        assertEquals(listOf(0xFF00FF00.toInt(), 0xFFFFFFFF.toInt(), 0xFF00FF00.toInt()), (0..2).map { out.getPixel(it, 0) })
    }

    private fun assertNotNullBitmap(b: android.graphics.Bitmap?): android.graphics.Bitmap = kotlin.test.assertNotNull(b)
}
