package com.programmersbox.mangaworld

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun collatzSequences() {
        // if odd == x * 3 + 1
        // if even == x / 2

        var x = 255

        repeat(10) {
            println(x)
            x = if (x % 2L == 0L) {
                x / 2
            } else {
                3 * x + 1
            }
        }

        println(x)

    }
}