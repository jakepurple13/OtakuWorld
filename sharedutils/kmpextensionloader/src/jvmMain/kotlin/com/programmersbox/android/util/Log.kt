package android.util

object Log {
    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6

    @JvmStatic fun v(tag: String, msg: String): Int { println("V/$tag: $msg"); return 0 }
    @JvmStatic fun v(tag: String, msg: String, tr: Throwable): Int { println("V/$tag: $msg\n${tr.stackTraceToString()}"); return 0 }
    @JvmStatic fun d(tag: String, msg: String): Int { println("D/$tag: $msg"); return 0 }
    @JvmStatic fun d(tag: String, msg: String, tr: Throwable): Int { println("D/$tag: $msg\n${tr.stackTraceToString()}"); return 0 }
    @JvmStatic fun i(tag: String, msg: String): Int { println("I/$tag: $msg"); return 0 }
    @JvmStatic fun i(tag: String, msg: String, tr: Throwable): Int { println("I/$tag: $msg\n${tr.stackTraceToString()}"); return 0 }
    @JvmStatic fun w(tag: String, msg: String): Int { println("W/$tag: $msg"); return 0 }
    @JvmStatic fun w(tag: String, msg: String, tr: Throwable): Int { println("W/$tag: $msg\n${tr.stackTraceToString()}"); return 0 }
    @JvmStatic fun w(tag: String, tr: Throwable): Int { println("W/$tag: ${tr.message}\n${tr.stackTraceToString()}"); return 0 }
    @JvmStatic fun e(tag: String, msg: String): Int { System.err.println("E/$tag: $msg"); return 0 }
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable): Int { System.err.println("E/$tag: $msg\n${tr.stackTraceToString()}"); return 0 }
    @JvmStatic fun wtf(tag: String, msg: String): Int { System.err.println("WTF/$tag: $msg"); return 0 }
    @JvmStatic fun wtf(tag: String, msg: String, tr: Throwable): Int { System.err.println("WTF/$tag: $msg\n${tr.stackTraceToString()}"); return 0 }
}
