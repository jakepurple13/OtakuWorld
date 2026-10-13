// OTAKU: copy of mihonapp/mihon JavaScriptEngine with Metro annotations removed.
package eu.kanade.tachiyomi.network

import android.content.Context
import app.cash.quickjs.QuickJs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("UNUSED", "UNCHECKED_CAST")
class JavaScriptEngine(context: Context) {
    /** @since extensions-lib 1.4 */
    suspend fun <T> evaluate(script: String): T = withContext(Dispatchers.Default) {
        QuickJs.create().use { it.evaluate(script) as T }
    }
}
