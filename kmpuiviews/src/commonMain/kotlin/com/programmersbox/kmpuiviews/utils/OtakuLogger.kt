package com.programmersbox.kmpuiviews.utils

object OtakuLogger : Logger {
    var shouldPrintLogs = false

    private var logger: Logger = DefaultLogger()

    fun init(logger: Logger) {
        this.logger = logger
    }

    override fun printLog(message: Any?) {
        logger.printLog(message)
    }

    override fun v(tag: String, message: Any?) {
        logger.v(tag, message)
    }

    override fun d(tag: String, message: Any?) {
        logger.d(tag, message)
    }

    override fun i(tag: String, message: Any?) {
        logger.i(tag, message)
    }

    override fun w(tag: String, message: Any?) {
        logger.w(tag, message)
    }

    override fun e(tag: String, message: Any?) {
        logger.e(tag, message)
    }

    override fun wtf(tag: String, message: Any?) {
        logger.wtf(tag, message)
    }

    override fun v(tag: String, message: Any?, tr: Throwable?) {
        logger.v(tag, message, tr)
    }

    override fun d(tag: String, message: Any?, tr: Throwable?) {
        logger.d(tag, message, tr)
    }

    override fun i(tag: String, message: Any?, tr: Throwable?) {
        logger.i(tag, message, tr)
    }

    override fun w(tag: String, message: Any?, tr: Throwable?) {
        logger.w(tag, message, tr)
    }

    override fun e(tag: String, message: Any?, tr: Throwable?) {
        logger.e(tag, message, tr)
    }

    override fun wtf(tag: String, message: Any?, tr: Throwable?) {
        logger.wtf(tag, message, tr)
    }

    override fun log(priority: Int, tag: String, message: Any?) {
        logger.log(priority, tag, message)
    }

    override fun log(priority: Int, tag: String, message: String?, tr: Throwable?) {
        logger.log(priority, tag, message, tr)
    }
}

inline fun printLogs(block: () -> Any?) {
    if (OtakuLogger.shouldPrintLogs) OtakuLogger.printLog(block())
}

interface Logger {
    fun printLog(message: Any?)
    fun v(tag: String, message: Any?) = printLog(message)
    fun v(tag: String, message: Any?, tr: Throwable?) = printLog(message)
    fun d(tag: String, message: Any?) = printLog(message)
    fun d(tag: String, message: Any?, tr: Throwable?) = printLog(message)
    fun i(tag: String, message: Any?) = printLog(message)
    fun i(tag: String, message: Any?, tr: Throwable?) = printLog(message)
    fun w(tag: String, message: Any?) = printLog(message)
    fun w(tag: String, message: Any?, tr: Throwable?) = printLog(message)
    fun e(tag: String, message: Any?) = printLog(message)
    fun e(tag: String, message: Any?, tr: Throwable?) = printLog(message)
    fun wtf(tag: String, message: Any?) = printLog(message)
    fun wtf(tag: String, message: Any?, tr: Throwable?) = printLog(message)
    fun log(priority: Int, tag: String, message: Any?) = printLog(message)
    fun log(priority: Int, tag: String, message: String?, tr: Throwable?) = printLog(message)
}

class DefaultLogger : Logger {
    override fun printLog(message: Any?) {
        println(message)
    }
}