// OTAKU: hand-written shim. Mihon extensions call app.cash.quickjs.QuickJs (quickjs-android); this app already ships Zipline's libquickjs.so, so delegate to app.cash.zipline.QuickJs to avoid a native-library collision.
package app.cash.quickjs

import java.io.Closeable

class QuickJs private constructor(
    private val delegate: app.cash.zipline.QuickJs,
) : Closeable {
    fun evaluate(script: String): Any? = delegate.evaluate(script, "?")

    fun evaluate(script: String, fileName: String): Any? = delegate.evaluate(script, fileName)

    fun compile(sourceCode: String, fileName: String): ByteArray = delegate.compile(sourceCode, fileName)

    fun execute(bytecode: ByteArray): Any? = delegate.execute(bytecode)

    override fun close() = delegate.close()

    companion object {
        @JvmStatic
        fun create(): QuickJs = QuickJs(app.cash.zipline.QuickJs.create())
    }
}
