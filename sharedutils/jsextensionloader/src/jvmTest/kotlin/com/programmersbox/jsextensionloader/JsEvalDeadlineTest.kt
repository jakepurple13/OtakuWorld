package com.programmersbox.jsextensionloader

import app.cash.zipline.QuickJs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.milliseconds

class JsEvalDeadlineTest {

    @Test
    fun interruptsALoopingScript() {
        val quickJs = QuickJs.create()
        try {
            val deadline = JsEvalDeadline(100.milliseconds)
            quickJs.interruptHandler = deadline
            assertFails { deadline.guard { quickJs.evaluate("while (true) {}", "loop.js") } }
            // The context stays usable for the next evaluation.
            assertEquals(2, deadline.guard { quickJs.evaluate("1 + 1", "ok.js") })
        } finally {
            quickJs.close()
        }
    }
}
