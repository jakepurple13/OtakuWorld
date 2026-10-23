// Copied from aniyomiorg/aniyomi 97414446b8a95994c72dd33c41c971a89d4d25b8, Apache-2.0. Local edits marked with "OTAKU:".
package eu.kanade.tachiyomi.network

import android.content.Context
import app.cash.quickjs.QuickJs
import tachiyomi.core.common.util.lang.withIOContext

/**
 * Util for evaluating JavaScript in sources.
 */
@Suppress("UNUSED", "UNCHECKED_CAST")
class JavaScriptEngine(context: Context) {

    /**
     * Evaluate arbitrary JavaScript code and get the result as a primtive type
     * (e.g., String, Int).
     *
     * @since extensions-lib 1.4
     * @param script JavaScript to execute.
     * @return Result of JavaScript code as a primitive type.
     */
    suspend fun <T> evaluate(script: String): T = withIOContext {
        QuickJs.create().use {
            it.evaluate(script) as T
        }
    }
}
