package com.programmersbox.uiviews

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

        val request = Request.Builder()
            .url("https://github.com/jakepurple13/OtakuWorld/releases/latest")
            .get()
            .build()
        val response = OkHttpClient().newCall(request).execute()
        val f = response.request().url()//if (response.code == 200) response.body!!.string() else null

        println(f)
    }
}